package com.rodbate.rpc.netty;




import com.rodbate.rpc.ChannelEventListener;
import com.rodbate.rpc.RpcClient;
import com.rodbate.rpc.RpcHook;
import com.rodbate.rpc.common.Pair;
import com.rodbate.rpc.common.RpcCommandHelper;
import com.rodbate.rpc.exception.RpcConnectException;
import com.rodbate.rpc.exception.RpcSendRequestException;
import com.rodbate.rpc.exception.RpcTimeoutException;
import com.rodbate.rpc.exception.RpcTooMuchRequestException;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import static com.rodbate.rpc.common.CommonUtil.*;



public class NettyRpcClient extends NettyRpcAbstract implements RpcClient {


    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcClient.class);


    private final Bootstrap bootstrap = new Bootstrap();

    private final NettyClientConfig nettyClientConfig;
    private final EventLoopGroup eventLoopGroupSelector;


    private final ExecutorService publicExecutor;

    private final ChannelEventListener channelEventListener;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private RpcHook rpcHook;

    private final Timer timer = new Timer("ClientScanResponseMapTimer", true);


    private final AtomicReference<List<String>> nameServerAddressList = new AtomicReference<>();
    private final AtomicReference<String> nameServerAddressChoose = new AtomicReference<>();
    private final AtomicInteger nameServerIndex = new AtomicInteger(initIndexValue());



    private final Lock nameServerChannelLock = new ReentrantLock();


    private final ConcurrentHashMap<String, ChannelWrapper> addressToChannelWrapperMap =
            new ConcurrentHashMap<>();
    private final Lock channelMapLock = new ReentrantLock();

    //ms
    private final static long LOCK_TIMEOUT_MILLIS = 3000;


    public NettyRpcClient(NettyClientConfig nettyClientConfig) {
        this(nettyClientConfig, null);
    }


    public NettyRpcClient(NettyClientConfig nettyClientConfig, ChannelEventListener channelEventListener) {
        super(nettyClientConfig.getClientSemaphoreAsyncValue(), nettyClientConfig.getClientSemaphoreOneWayValue());
        this.nettyClientConfig = nettyClientConfig;
        this.channelEventListener = channelEventListener;

        this.eventLoopGroupSelector = new NioEventLoopGroup(1,
                new ThreadFactory() {

                    private AtomicInteger index = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ClientSelectorEventGroup-Thread-" + index.getAndIncrement());
                    }
                });

        this.publicExecutor = Executors.newFixedThreadPool(nettyClientConfig.getClientCallbackThreadNums(),
                new ThreadFactory() {

                    private AtomicInteger index = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ClientPublicExecutor-Thread-" + index.getAndIncrement());
                    }
                });
    }

    @Override
    public ChannelEventListener getChannelEventListener() {
        return this.channelEventListener;
    }

    @Override
    public RpcHook getRpcHook() {
        return this.rpcHook;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.publicExecutor;
    }

    @Override
    public boolean isChannelWritable(String address) {
        ChannelWrapper channelWrapper = addressToChannelWrapperMap.get(address);

        return channelWrapper != null && channelWrapper.isWritable();
    }


    private int initIndexValue() {
        Random r = new Random();

        return (Math.abs(r.nextInt()) % 999);
    }

    @Override
    public void start() {

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreadNums(),
                new ThreadFactory() {

                    private AtomicInteger index = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ClientCodecEventGroup-Thread-" + index.getAndIncrement());
                    }
                });


        this.bootstrap.group(eventLoopGroupSelector).channel(NioSocketChannel.class)

                .option(ChannelOption.SO_KEEPALIVE, false)

                .option(ChannelOption.TCP_NODELAY, true)

                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis())

                .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize())

                .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize())

                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(
                                defaultEventExecutorGroup,
                                new RpcNettyEncoder(),
                                new RpcNettyDecoder(),
                                new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelIdleTimeSeconds()),
                                new NettyClientManageHandler(),
                                new NettyClientHandler()
                        );
                    }
                });


        if (channelEventListener != null)
        {
            eventExecutor.start();
        }

        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                scanResponseFutureMap();
            }
        }, 4000, 1000);
    }

    public Bootstrap getBootstrap()
    {
        return bootstrap;
    }

    @Override
    public void shutdown() {

        if (timer != null)
        {
            timer.cancel();
        }

        addressToChannelWrapperMap.clear();

        for (ChannelWrapper cw : addressToChannelWrapperMap.values())
        {
            closeChannel(cw.getChannel());
        }

        eventLoopGroupSelector.shutdownGracefully();

        if (defaultEventExecutorGroup != null)
        {
            defaultEventExecutorGroup.shutdownGracefully();
        }

        if (eventExecutor != null)
        {
            eventExecutor.shutdown();
        }

        if (publicExecutor != null)
        {
            publicExecutor.shutdown();
        }

    }

    @Override
    public void registerRpcHook(RpcHook rpcHook) {
        this.rpcHook = rpcHook;
    }

    @Override
    public void registerProcessor(int requestCode, NettyRpcRequestProcessor processor, ExecutorService service) {
        Objects.requireNonNull(processor, "processor require not null");

        if (service == null)
        {
            service = this.publicExecutor;
        }
        codeToProcessorMap.put(requestCode, new Pair<>(processor, service));
    }

    @Override
    public void registerDefaultProcessor(NettyRpcRequestProcessor processor, ExecutorService service) {
        Objects.requireNonNull(processor, "processor require not null");

        if (service == null)
        {
            service = this.publicExecutor;
        }
        this.defaultRequestProcessor = new Pair<>(processor, service);
    }


    @Override
    public void updateNameServerAddressList(List<String> list) {

        List<String> oldList = nameServerAddressList.get();

        boolean update = false;

        if (list != null && !list.isEmpty())
        {

            if (oldList == null)
            {
                update = true;
            }
            else if (oldList.size() != list.size())
            {
                update = true;
            }
            else
            {
                for (int i = 0; i < list.size(); i++) {
                    if (!oldList.contains(list.get(i)))
                    {
                        update = true;
                        break;
                    }
                }
            }
        }

        if (update)
        {
            Collections.shuffle(list);
            nameServerAddressList.set(list);
        }
    }

    @Override
    public List<String> getNameServerAddressList() {
        return nameServerAddressList.get();
    }

    @Override
    public RpcCommand invokeSync(String address, RpcCommand request)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException, RpcConnectException {
        return invokeSync(address, request, 0);
    }

    @Override
    public RpcCommand invokeSync(String address, RpcCommand request, long timeoutMillis)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException, RpcConnectException {

        Channel channel = getAndCreateChannel(address);

        if (channel != null && channel.isActive())
        {
            try {
                if (rpcHook != null) {
                    rpcHook.doBeforeRequest(address, request);
                }

                RpcCommand response = invokeSyncImpl(channel, request, timeoutMillis);

                if (rpcHook != null) {
                    rpcHook.doAfterResponse(address, request, response);
                }

                return response;
            } catch (RpcTimeoutException e){
                //
                if (nettyClientConfig.isClientCloseSocketIfTimeout())
                {
                    LOGGER.info("invokeSync : send request timeout <{}>(ms)", nettyClientConfig.getConnectTimeoutMillis());
                    closeChannel(address, channel);
                }
                throw e;
            } catch (RpcSendRequestException e) {
                //
                LOGGER.info("invokeSync : send request exception,  close the channel <{}>", address);
                closeChannel(address, channel);
                throw e;
            }
        }
        else
        {
            closeChannel(address, channel);
            throw new RpcConnectException(address);
        }
    }

    @Override
    public void invokeAsync(String address, RpcCommand request, long timeoutMillis, InvokeCallback callback)
            throws InterruptedException, RpcTooMuchRequestException, RpcSendRequestException, RpcConnectException {
        final Channel channel = this.getAndCreateChannel(address);
        if (channel != null && channel.isActive()) {
            try {
                if (this.rpcHook != null) {
                    this.rpcHook.doBeforeRequest(address, request);
                }
                this.invokeAsyncImpl(channel, request, timeoutMillis, callback);
            } catch (RpcSendRequestException e) {
                LOGGER.info("invokeAsync: send request exception, close the channel <{}>", address);
                this.closeChannel(address, channel);
                throw e;
            }
        } else {
            this.closeChannel(address, channel);
            throw new RpcConnectException(address);
        }
    }

    @Override
    public void invokeOneWay(String address, RpcCommand request, long timeoutMillis)
            throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException, RpcConnectException {
        final Channel channel = this.getAndCreateChannel(address);
        if (channel != null && channel.isActive()) {
            try {
                if (this.rpcHook != null) {
                    this.rpcHook.doBeforeRequest(address, request);
                }
                this.invokeOneWayImpl(channel, request, timeoutMillis);
            } catch (RpcSendRequestException e) {
                LOGGER.info("invokeAsync: send request exception, close the channel <{}>", address);
                this.closeChannel(address, channel);
                throw e;
            }
        } else {
            this.closeChannel(address, channel);
            throw new RpcConnectException(address);
        }
    }


    public void closeChannel(String address, Channel channel) {
        if (channel == null)
        {
            return;
        }

        String remoteAddress = address == null ? getRemoteAddressFromChannel(channel) : address;

        try {

            if (channelMapLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
            {
                try{
                    boolean removeItemFromMap = true;

                    ChannelWrapper cw = addressToChannelWrapperMap.get(remoteAddress);

                    LOGGER.info("closeChannel : begin to close channel <{}>, Found : {}", remoteAddress, cw != null);

                    if (cw == null)
                    {
                        removeItemFromMap = false;
                        LOGGER.info("closeChannel : the channel <{}> has removed from the channel map", remoteAddress);
                    }
                    else if (cw.getChannel() != channel)
                    {
                        removeItemFromMap = false;
                        LOGGER.info("closeChannel : the channel <{}> has closed before,new channel has created again, skip...", remoteAddress);
                    }

                    if (removeItemFromMap)
                    {
                        addressToChannelWrapperMap.remove(remoteAddress);
                        LOGGER.info("closeChannel : the channel <{}>  remove from the channel map successfully ", remoteAddress);
                        RpcCommandHelper.closeChannel(channel);
                    }

                } catch (Exception e){
                    LOGGER.info(e.getMessage(), e);
                } finally {
                    channelMapLock.unlock();
                }
            }
            else
            {
                LOGGER.info("closeChannel : try to lock channelMapLock timeout [{}](ms)", LOCK_TIMEOUT_MILLIS);
            }

        } catch (InterruptedException e) {
            LOGGER.info("closeChannel : throw InterruptedException ", e);
        }
    }


    public void closeChannel(Channel channel)
    {
        if (channel == null) return;

        try {
            if (channelMapLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
            {
                try{

                    ChannelWrapper cw = null;
                    String address = null;

                    for (Map.Entry<String, ChannelWrapper> entry : addressToChannelWrapperMap.entrySet())
                    {
                        if (entry.getValue().getChannel() == channel)
                        {
                            cw = entry.getValue();
                            address = entry.getKey();
                            break;
                        }
                    }

                    if (cw != null)
                    {
                        addressToChannelWrapperMap.remove(address);
                        LOGGER.info("closeChannel : the channel <{}>  remove from the channel map successfully ", address);
                        RpcCommandHelper.closeChannel(channel);
                    }


                } catch (Exception e){
                    LOGGER.info(e.getMessage(), e);
                } finally {
                    channelMapLock.unlock();
                }
            }
            else
            {
                LOGGER.info("closeChannel : try to lock channelMapLock timeout [{}](ms)", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            LOGGER.info("closeChannel : throw InterruptedException ", e);
        }
    }

    private Channel getAndCreateChannel(String address) throws InterruptedException
    {
        if (address == null)
        {
            return getAndCreateNameServerChannel();
        }

        ChannelWrapper channelWrapper = addressToChannelWrapperMap.get(address);

        if (channelWrapper != null && channelWrapper.isOk())
        {
            return channelWrapper.getChannel();
        }

        return createNewChannel(address);
    }



    private Channel getAndCreateNameServerChannel() throws InterruptedException {

        String chosenAddress = nameServerAddressChoose.get();

        if (chosenAddress != null)
        {
            ChannelWrapper cw = addressToChannelWrapperMap.get(chosenAddress);
            if (cw != null && cw.isOk())
            {
                return cw.getChannel();
            }
        }

        final List<String> addressList = nameServerAddressList.get();

        if (nameServerChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        {
            try
            {
                chosenAddress = nameServerAddressChoose.get();
                if (chosenAddress != null)
                {
                    ChannelWrapper cw = addressToChannelWrapperMap.get(chosenAddress);
                    if (cw != null && cw.isOk())
                    {
                        return cw.getChannel();
                    }
                }

                if (addressList != null && !addressList.isEmpty())
                {
                    for (int i = 0; i < addressList.size(); i++)
                    {
                        int index = nameServerIndex.incrementAndGet();
                        index = Math.abs(index);

                        index = index % addressList.size();

                        String newAddress = addressList.get(index);
                        nameServerAddressChoose.set(newAddress);

                        Channel newChannel = createNewChannel(newAddress);

                        if (newChannel != null)
                        {
                            return newChannel;
                        }
                    }
                }


            } catch (Exception e){
                LOGGER.info(e.getMessage(), e);
            } finally {
                nameServerChannelLock.unlock();
            }

        }
        else
        {
            LOGGER.info("getAndCreateNameServerChannel try to lock name server, but timeout {}(ms)", LOCK_TIMEOUT_MILLIS);
        }


        return null;
    }


    private Channel createNewChannel(String address) throws InterruptedException {

        ChannelWrapper cw = addressToChannelWrapperMap.get(address);

        if (cw != null && cw.isOk())
        {
            return cw.getChannel();
        }

        if (channelMapLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        {
            try
            {
                boolean createNewConn;

                cw = addressToChannelWrapperMap.get(address);

                if (cw != null)
                {
                    if (cw.isOk())
                    {
                        return cw.getChannel();
                    }
                    else if (!cw.getChannelFuture().isDone())
                    {
                        createNewConn = false;
                    }
                    else
                    {
                        addressToChannelWrapperMap.remove(address);
                        createNewConn = true;
                    }
                }
                else
                {
                    createNewConn = true;
                }

                if (createNewConn)
                {
                    ChannelFuture channelFuture = this.bootstrap.connect(stringAddressToSocketAddress(address));

                    LOGGER.info("createNewChannel : connect the remote channel <{}> asynchronously ", address);

                    cw = new ChannelWrapper(channelFuture);

                    addressToChannelWrapperMap.put(address, cw);
                }


            } catch (Exception e) {
                LOGGER.info(e.getMessage(), e);
            } finally {
                channelMapLock.unlock();
            }
        }
        else
        {
            LOGGER.info("createNewChannel try to lock addressToChannelWrapperMap, but timeout {}(ms)", LOCK_TIMEOUT_MILLIS);
        }

        if (cw != null)
        {
            ChannelFuture channelFuture = cw.getChannelFuture();

            if (channelFuture.awaitUninterruptibly(nettyClientConfig.getConnectTimeoutMillis()))
            {
                if (cw.isOk())
                {
                    LOGGER.info("createNewChannel : connect the remote host [{}] successfully ", address);
                    return cw.getChannel();
                }
                else
                {
                    LOGGER.info("createNewChannel : connect the remote host [{}] failed ", address);
                }
            }
            else
            {
                LOGGER.info("createNewChannel : connect the remote host [{}] timeout , [{}](ms) ",
                        address, nettyClientConfig.getConnectTimeoutMillis());
            }

        }

        return null;
    }


    class ChannelWrapper {

        private final ChannelFuture channelFuture;

        public ChannelWrapper(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        public boolean isOk() {
            return channelFuture.channel() != null && channelFuture.channel().isActive();
        }

        public boolean isWritable(){
            return getChannel().isWritable();
        }

        public Channel getChannel(){
            return channelFuture.channel();
        }

        public ChannelFuture getChannelFuture() {
            return channelFuture;
        }
    }

    class NettyClientHandler extends SimpleChannelInboundHandler<RpcCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcCommand msg) throws Exception {
            processReceiveMessageFromNetty(ctx, msg);
        }
    }

    class NettyClientManageHandler extends ChannelDuplexHandler {

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise)
                throws Exception {
            final String local = localAddress == null ? "UNKNOW" : localAddress.toString();
            final String remote = remoteAddress == null ? "UNKNOW" : remoteAddress.toString();
            LOGGER.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);

            super.connect(ctx, remoteAddress, localAddress, promise);
            //connect
            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.CONNECT,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }

        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {

            LOGGER.info("Netty Client Pipeline : disconnect <{}>", getRemoteAddressFromChannel(ctx.channel()));
            RpcCommandHelper.closeChannel(ctx.channel());
            super.disconnect(ctx, promise);
            //connect
            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.CLOSE,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }

        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            LOGGER.info("Netty Client Pipeline : close <{}>", getRemoteAddressFromChannel(ctx.channel()));
            RpcCommandHelper.closeChannel(ctx.channel());
            super.close(ctx, promise);



            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.CLOSE,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            LOGGER.info("Netty Client Pipeline : exceptionCaught <{}>", getRemoteAddressFromChannel(ctx.channel()));
            LOGGER.info("Netty Client Pipeline : throw an exception {}", cause.getMessage());
            RpcCommandHelper.closeChannel(ctx.channel());
            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.EXCEPTION,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }

        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent)
            {
                IdleStateEvent e = (IdleStateEvent) evt;

                if (e.state() == IdleState.ALL_IDLE)
                {
                    LOGGER.info("Netty Client Pipeline : Idle exception <{}>", getRemoteAddressFromChannel(ctx.channel()));
                    RpcCommandHelper.closeChannel(ctx.channel());
                    if (channelEventListener != null)
                    {
                        registerChannelEvent(new ChannelEvent(ChannelEventType.IDLE,
                                getRemoteAddressFromChannel(ctx.channel()),
                                ctx.channel()));
                    }
                }
            }
            ctx.fireUserEventTriggered(evt);
        }
    }

}
