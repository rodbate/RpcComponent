package com.rodbate.rpc.netty;


import com.rodbate.rpc.ChannelEventListener;
import com.rodbate.rpc.RpcHook;
import com.rodbate.rpc.RpcServer;
import com.rodbate.rpc.common.Pair;
import com.rodbate.rpc.common.RpcCommandHelper;
import com.rodbate.rpc.exception.RpcSendRequestException;
import com.rodbate.rpc.exception.RpcTimeoutException;
import com.rodbate.rpc.exception.RpcTooMuchRequestException;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;



import static com.rodbate.rpc.common.CommonUtil.*;


public class NettyRpcServer extends NettyRpcAbstract implements RpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcServer.class);

    //netty server 启动器
    private final ServerBootstrap serverBootstrap;

    //netty boss 线程池
    private final EventLoopGroup eventLoopGroupBoss;

    //netty io selector 线程池
    private final EventLoopGroup eventLoopGroupSelector;

    //netty server config 配置
    private final NettyServerConfig nettyServerConfig;


    private final ExecutorService publicExecutor;
    private final ChannelEventListener channelEventListener;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private final Timer timer = new Timer("ServerScanResponseMapTimer", true);


    private RpcHook rpcHook;

    private int port = 0;


    public NettyRpcServer(NettyServerConfig config) {
        this(config, null);
    }

    public NettyRpcServer(NettyServerConfig config, ChannelEventListener channelEventListener)
    {
        super(config.getServerSemaphoreAsyncValue(), config.getServerSemaphoreOneWayValue());
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = config;
        this.channelEventListener = channelEventListener;

        int publicThreadNums = config.getServerCallbackExecutorThreads();

        if (publicThreadNums <= 0)
        {
            publicThreadNums = 4;
        }

        this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {

            private AtomicInteger threadIndex = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerPublicExecutor-Thread-" + threadIndex.getAndIncrement());
            }
        });

        this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerBoss-Thread-" + threadIndex.getAndIncrement());
            }
        });


        this.eventLoopGroupSelector = new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerSelector-Thread-" + threadIndex.getAndIncrement());
            }
        });

    }

    @Override
    public ChannelEventListener getChannelEventListener() {
        return channelEventListener;
    }

    @Override
    public RpcHook getRpcHook() {
        return rpcHook;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.publicExecutor;
    }

    @Override
    public int localListenPort() {
        return this.port;
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
    public void start() {

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyServerConfig.getServerWorkThreads(),
                new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "DefaultEventExecutorGroup-Thread-" + threadIndex.getAndIncrement());
                    }
                });


        ServerBootstrap bootstrap = this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)

                .channel(NioServerSocketChannel.class)

                .option(ChannelOption.SO_BACKLOG, 1024)

                .option(ChannelOption.SO_KEEPALIVE, false)

                .option(ChannelOption.TCP_NODELAY, true)

                .option(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())

                .option(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())

                .option(ChannelOption.SO_REUSEADDR, true)

                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(
                                defaultEventExecutorGroup,
                                new RpcNettyEncoder(),
                                new RpcNettyDecoder(),
                                new IdleStateHandler(0, 0, nettyServerConfig.getServerChannelIdleTimeSeconds()),
                                new NettyServerManagerHandler(),
                                new NettyServerHandler()
                        );
                    }
                });

        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable())
        {
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }


        try {

            this.port = nettyServerConfig.getListenPort();

            bootstrap.bind(new InetSocketAddress(nettyServerConfig.getHostName(),
                    nettyServerConfig.getListenPort())).sync().channel();

            LOGGER.info("Rpc Netty Server start successfully , bind port [{}]", this.port);

        } catch (InterruptedException e) {

            LOGGER.info("Netty Server bootstrap throw exception", e);
        }
        if (channelEventListener != null)
        {
            eventExecutor.start();
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                scanResponseFutureMap();
            }

        }, 4000, 1000);
    }

    @Override
    public void shutdown() {

        try {
            if (timer != null) {
                timer.cancel();
            }

            eventLoopGroupBoss.shutdownGracefully();

            eventLoopGroupSelector.shutdownGracefully();

            if (eventExecutor != null) {
                eventExecutor.shutdown();
            }

            defaultEventExecutorGroup.shutdownGracefully();

            publicExecutor.shutdown();
        } catch (Exception e){

            LOGGER.info("Rpc Netty Server shutdown occur exception", e);
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

        this.codeToProcessorMap.put(requestCode, new Pair<>(processor, service));
    }

    @Override
    public RpcCommand invokeSync(Channel channel, RpcCommand request)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException {
        return invokeSyncImpl(channel, request);
    }

    @Override
    public RpcCommand invokeSync(Channel channel, RpcCommand request, long timeoutMillis)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException {
        return invokeSyncImpl(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(Channel channel, RpcCommand request, long timeoutMillis, InvokeCallback callback)
            throws InterruptedException, RpcTooMuchRequestException, RpcSendRequestException {
        invokeAsyncImpl(channel, request, timeoutMillis, callback);
    }

    @Override
    public void invokeOneWay(Channel channel, RpcCommand request, long timeoutMillis)
            throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException {
        invokeOneWayImpl(channel, request, timeoutMillis);
    }


    class NettyServerHandler extends SimpleChannelInboundHandler<RpcCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcCommand msg) throws Exception {
            processReceiveMessageFromNetty(ctx, msg);
        }
    }



    class NettyServerManagerHandler extends ChannelDuplexHandler {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("Netty Server Pipeline : channelRegistered <{}>", getRemoteAddressFromChannel(ctx.channel()));
            super.channelRegistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("Netty Server Pipeline : channelActive <{}>", getRemoteAddressFromChannel(ctx.channel()));

            //connect
            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.CONNECT,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("Netty Server Pipeline : channelInactive <{}>", getRemoteAddressFromChannel(ctx.channel()));

            //close channel
            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.CLOSE,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }

            super.channelInactive(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("Netty Server Pipeline : channelUnregistered <{}>", getRemoteAddressFromChannel(ctx.channel()));
            super.channelUnregistered(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent)
            {
                IdleStateEvent e = (IdleStateEvent) evt;

                if (e.state() == IdleState.ALL_IDLE)
                {
                    LOGGER.info("Netty Server Pipeline : Idle exception <{}>", getRemoteAddressFromChannel(ctx.channel()));
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

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            LOGGER.info("Netty Server Pipeline : exceptionCaught <{}>", getRemoteAddressFromChannel(ctx.channel()));
            LOGGER.info("Netty Server Pipeline : throw an exception {}", cause.getMessage());

            if (channelEventListener != null)
            {
                registerChannelEvent(new ChannelEvent(ChannelEventType.EXCEPTION,
                        getRemoteAddressFromChannel(ctx.channel()),
                        ctx.channel()));
            }
            RpcCommandHelper.closeChannel(ctx.channel());
        }
    }
}
