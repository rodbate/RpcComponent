package com.rodbate.rpc.netty;


import com.rodbate.rpc.ChannelEventListener;
import com.rodbate.rpc.RpcHook;
import com.rodbate.rpc.common.CommonUtil;
import com.rodbate.rpc.common.Pair;
import com.rodbate.rpc.common.SemaphoreReleaseOnlyOnce;
import com.rodbate.rpc.common.ServiceThread;
import com.rodbate.rpc.exception.RpcCommandException;
import com.rodbate.rpc.exception.RpcSendRequestException;
import com.rodbate.rpc.exception.RpcTimeoutException;
import com.rodbate.rpc.exception.RpcTooMuchRequestException;
import com.rodbate.rpc.protocol.RpcCommand;
import com.rodbate.rpc.protocol.RpcResponseCommandType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.rodbate.rpc.common.CommonUtil.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;



public abstract class NettyRpcAbstract {


    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcAbstract.class);


    protected final Semaphore semaphoreAsync;

    protected final Semaphore semaphoreOneWay;

    //每个请求对应的响应future
    protected final ConcurrentHashMap<Integer, ResponseFuture> seqToResponseFutureMap =
            new ConcurrentHashMap<>(256);

    //请求处理器集合
    protected final ConcurrentHashMap<Integer, Pair<NettyRpcRequestProcessor, ExecutorService>> codeToProcessorMap =
            new ConcurrentHashMap<>(128);

    //默认处理器
    protected Pair<NettyRpcRequestProcessor, ExecutorService> defaultRequestProcessor;


    public NettyRpcAbstract(final int asyncPermits, final int oneWayPermits) {
        //fair lock
        this.semaphoreAsync = new Semaphore(asyncPermits, true);
        this.semaphoreOneWay = new Semaphore(oneWayPermits, true);
    }

    private final ChannelEventExecutor eventExecutor = new ChannelEventExecutor();



    public abstract ChannelEventListener getChannelEventListener();



    public void registerChannelEvent(final ChannelEvent event)
    {
        this.eventExecutor.putChannelEvent(event);
    }



    public void processReceiveMessageFromNetty(final ChannelHandlerContext ctx, final RpcCommand msg)
    {
        final RpcCommand cmd = msg;

        if (cmd != null)
        {
            switch (cmd.getCmdType())
            {
                case REQUEST_COMMAND:
                    processRequestCommand(ctx, cmd);
                    break;
                case RESPONSE_COMMAND:
                    processResponseCommand(ctx, cmd);
                    break;
                default:
                    break;
            }
        }
    }


    public void processRequestCommand(final ChannelHandlerContext ctx, final RpcCommand cmd)
    {
        final Pair<NettyRpcRequestProcessor, ExecutorService> matched = codeToProcessorMap.get(cmd.getCode());

        final Pair<NettyRpcRequestProcessor, ExecutorService> pair = matched == null ? defaultRequestProcessor : matched;

        final int seq = cmd.getSeq();

        if (pair != null)
        {

            Runnable runnable = () -> {

                try {

                    RpcHook rpcHook = getRpcHook();

                    if (rpcHook != null) {
                        rpcHook.doBeforeRequest(getRemoteAddressFromChannel(ctx.channel()), cmd);
                    }

                    RpcCommand response = pair.getKey().processRequest(ctx, cmd);

                    if (rpcHook != null) {
                        rpcHook.doAfterResponse(getRemoteAddressFromChannel(ctx.channel()), cmd, response);
                    }

                    if (!cmd.isRpcOneWay())
                    {
                        if (response != null)
                        {
                            response.setSeq(seq);
                            response.markRpcResponse();
                            ctx.writeAndFlush(response);
                        }
                    }

                } catch (Exception e) {
                    //
                    if (!cmd.isRpcOneWay())
                    {
                        RpcCommand response =
                                RpcCommand.createResponseCommand(RpcResponseCommandType.SYSTEM_ERROR, e.getMessage());
                        response.setSeq(seq);
                        ctx.writeAndFlush(response);
                    }
                }

            };

            //rpc server拒绝请求
            if (pair.getKey().rejectRequest())
            {
                RpcCommand response =
                        RpcCommand.createResponseCommand(RpcResponseCommandType.SYSTEM_BUSY, "[SYSTEM BUSY] ==> reject request");
                response.setSeq(seq);
                ctx.writeAndFlush(response);
                return;
            }

            try {

                pair.getValue().submit(runnable);

            } catch (RejectedExecutionException e){
                //
                if (!cmd.isRpcOneWay())
                {
                    RpcCommand response =
                            RpcCommand.createResponseCommand(RpcResponseCommandType.SYSTEM_BUSY,
                                    "[SYSTEM BUSY] ==> overload wait for a while");
                    response.setSeq(seq);
                    ctx.writeAndFlush(response);
                }
            }

        }

        //not supported request processor
        else
        {
            String errorMsg = " request type " + cmd.getCode() + " not support ";
            RpcCommand responseCommand =
                    RpcCommand.createResponseCommand(RpcResponseCommandType.REQUEST_CODE_NOT_SUPPORT, errorMsg);
            responseCommand.setSeq(seq);
            ctx.writeAndFlush(responseCommand);
            LOGGER.info(CommonUtil.getRemoteAddressFromChannel(ctx.channel()) + errorMsg);
        }

    }


    public void processResponseCommand(ChannelHandlerContext ctx, RpcCommand cmd)
    {

        final int seq = cmd.getSeq();

        final ResponseFuture responseFuture = seqToResponseFutureMap.get(seq);

        if (responseFuture != null)
        {
            responseFuture.setResponseCmd(cmd);

            responseFuture.release();

            seqToResponseFutureMap.remove(seq);

            if (responseFuture.getCallback() != null)
            {

                ExecutorService executor = getCallbackExecutor();

                if (executor != null) {

                    executor.submit(() -> {

                        try {
                            responseFuture.executeCallback();
                        } catch (RpcCommandException e) {
                            LOGGER.info(" execute response callback throw exception {}", e.getMessage());
                        }
                    });
                }

                else
                {
                    try {
                        responseFuture.executeCallback();
                    } catch (RpcCommandException e) {
                        LOGGER.info(" execute response callback throw exception {}", e.getMessage());
                    }
                }

            }

            else
            {
                responseFuture.putResponse(cmd);
            }

        }

        else
        {
            LOGGER.info("receive response but no matched request ,channel [{}]", getRemoteAddressFromChannel(ctx.channel()));
        }

    }

    public void scanResponseFutureMap() {
        Iterator<Map.Entry<Integer, ResponseFuture>> iterator =
                this.seqToResponseFutureMap.entrySet().iterator();

        List<ResponseFuture> timeout = new LinkedList<>();

        while (iterator.hasNext())
        {
            ResponseFuture responseFuture = iterator.next().getValue();

            if (responseFuture.isTimeout())
            {
                responseFuture.release();
                timeout.add(responseFuture);
                iterator.remove();
                LOGGER.info(" remove timeout response future {}", responseFuture);
            }
        }

        for (ResponseFuture future : timeout)
        {
            try {
                future.executeCallback();
            } catch (RpcCommandException e) {
                //
            }
        }
    }


    public RpcCommand invokeSyncImpl(final Channel channel, final RpcCommand request)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException {
        return invokeSyncImpl(channel, request, 0);
    }

    public RpcCommand invokeSyncImpl(final Channel channel, final RpcCommand request, final long timeoutMillis)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException {

        final int seq = request.getSeq();

        try {

            ResponseFuture responseFuture = new ResponseFuture(seq, timeoutMillis, null, null);

            seqToResponseFutureMap.put(seq, responseFuture);

            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess())
                    {
                        responseFuture.setSendRequestOk(true);
                        return;
                    }
                    else
                    {
                        responseFuture.setSendRequestOk(false);
                    }

                    seqToResponseFutureMap.remove(seq);
                    responseFuture.setCause(future.cause());
                    responseFuture.putResponse(null);
                    LOGGER.info(" send a request command to channel <{}> failed", channel.remoteAddress());
                }
            });

            RpcCommand response;

            if (timeoutMillis == 0)
            {
                response = responseFuture.waitForResponse();
                if (response == null)
                {
                    if (!responseFuture.isSendRequestOk())
                    {
                        throw new RpcSendRequestException(getRemoteAddressFromChannel(channel), responseFuture.getCause());
                    }
                }
            }
            else
            {
                response = responseFuture.waitForResponse(timeoutMillis);
                if (response == null)
                {

                    if (responseFuture.isSendRequestOk())
                    {
                        throw new RpcTimeoutException(getRemoteAddressFromChannel(channel), timeoutMillis, responseFuture.getCause());
                    }
                    else
                    {
                        throw new RpcSendRequestException(getRemoteAddressFromChannel(channel), responseFuture.getCause());
                    }

                }
            }

            return response;

        } finally {
            seqToResponseFutureMap.remove(seq);
        }
    }


    public void invokeAsyncImpl(final Channel channel, final RpcCommand request, final long timeoutMillis,
                                      final InvokeCallback callback)
            throws InterruptedException, RpcTooMuchRequestException, RpcSendRequestException {

        final int seq = request.getSeq();
        boolean acquire = semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);

        if (acquire)
        {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);

            final ResponseFuture responseFuture = new ResponseFuture(seq, timeoutMillis, callback, once);

            seqToResponseFutureMap.put(seq, responseFuture);

            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {

                        if (future.isSuccess()) {
                            responseFuture.setSendRequestOk(true);
                            return;
                        } else {
                            responseFuture.setSendRequestOk(false);
                        }

                        seqToResponseFutureMap.remove(seq);
                        responseFuture.setCause(future.cause());
                        responseFuture.putResponse(null);

                        try {
                            responseFuture.executeCallback();
                        } catch (Throwable e) {
                            LOGGER.info("  response future execute callback throw exception", e);
                        } finally {
                            responseFuture.release();
                        }

                        LOGGER.info(" send a request command to channel <{}> failed", channel.remoteAddress());
                    }
                });
            }catch (Exception e){
                responseFuture.release();
                seqToResponseFutureMap.remove(seq);
                LOGGER.info(" send a request command to channel <" + getRemoteAddressFromChannel(channel) + "> throw exception ", e);
                throw new RpcSendRequestException(getRemoteAddressFromChannel(channel), e);
            }
        }

        else
        {
            String info = String.format("invokeAsyncImpl tryAcquire timeout %dms, waiting thread nums %d, semaphoreAsyncValue %d",
                    timeoutMillis, semaphoreAsync.getQueueLength(), semaphoreAsync.availablePermits());
            LOGGER.info(info);
            throw new RpcTooMuchRequestException(info);
        }

    }


    public void invokeOneWayImpl(final Channel channel, final RpcCommand request, final long timeoutMillis)
            throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException {

        request.markRpcOneWay();
        boolean tryAcquire = semaphoreOneWay.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);

        if (tryAcquire)
        {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneWay);

            try{

                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        once.release();
                        if (!future.isSuccess())
                        {
                            LOGGER.info(" send a request to channel <{}> failed ", getRemoteAddressFromChannel(channel));
                        }
                    }
                });

            } catch (Exception e){
                once.release();
                LOGGER.info(" send a request to channel <{}> failed ", getRemoteAddressFromChannel(channel));
                throw new RpcSendRequestException(getRemoteAddressFromChannel(channel), e);
            }
        }
        else
        {
            String info = String.format("semaphoreOneWay tryAcquire timeout %dms, waiting thread nums %d, semaphoreAsyncValue %d",
                    timeoutMillis, semaphoreOneWay.getQueueLength(), semaphoreOneWay.availablePermits());
            LOGGER.info(info);
            throw new RpcTooMuchRequestException(info);
        }
    }


    public abstract RpcHook getRpcHook();

    public abstract ExecutorService getCallbackExecutor();


    class ChannelEventExecutor extends ServiceThread {

        private final LinkedBlockingQueue<ChannelEvent> queue = new LinkedBlockingQueue<>(10000);


        public void putChannelEvent(final ChannelEvent event)
        {
            if (!queue.offer(event))
            {
                LOGGER.info("event queue is full, the size is {}", queue.size());
            }
        }

        @Override
        public void run() {

            LOGGER.info("server thread [{}] start ", this.getServiceName());

            ChannelEventListener eventListener = getChannelEventListener();

            while (!this.isStopped())
            {

                try {

                    ChannelEvent event = queue.poll(3000, TimeUnit.MILLISECONDS);

                    if (eventListener != null && event != null)
                    {

                        switch (event.getEventType())
                        {

                            case CONNECT:
                                eventListener.onChannelConnect(event.getRemoteAddress(), event.getChannel());
                                break;
                            case CLOSE:
                                eventListener.onChannelClose(event.getRemoteAddress(), event.getChannel());
                                break;
                            case EXCEPTION:
                                eventListener.onChannelException(event.getRemoteAddress(), event.getChannel());
                                break;
                            case IDLE:
                                eventListener.onChannelIdle(event.getRemoteAddress(), event.getChannel());
                                break;
                            default:
                                break;

                        }
                    }

                } catch (InterruptedException e) {
                    LOGGER.info(" service thread [{}] has an InterruptedException {}", this.getServiceName(), e.getMessage());
                }
            }

            LOGGER.info("server thread [{}] finish ", this.getServiceName());
        }


        @Override
        public String getServiceName() {
            return getClass().getSimpleName();
        }
    }


}
