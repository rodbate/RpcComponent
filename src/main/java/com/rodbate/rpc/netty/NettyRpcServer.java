package com.rodbate.rpc.netty;


import com.rodbate.rpc.ChannelEventListener;
import com.rodbate.rpc.RpcHook;
import com.rodbate.rpc.RpcServer;
import com.rodbate.rpc.exception.RpcSendRequestException;
import com.rodbate.rpc.exception.RpcTimeoutException;
import com.rodbate.rpc.exception.RpcTooMuchRequestException;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


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


    private RpcHook rpcHook;

    private int port = 0;



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
        return null;
    }

    @Override
    public RpcHook getRpcHook() {
        return null;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return null;
    }

    @Override
    public int localListenPort() {
        return 0;
    }

    @Override
    public void registerDefaultProcessor(NettyRpcRequestProcessor processor, ExecutorService service) {

    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerRpcHook(RpcHook rpcHook) {

    }

    @Override
    public void registerProcessor(int requestCode, NettyRpcRequestProcessor processor, ExecutorService service) {

    }

    @Override
    public RpcCommand invokeSync(Channel channel, RpcCommand request)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException {
        return null;
    }

    @Override
    public RpcCommand invokeSync(Channel channel, RpcCommand request, long timeoutMillis)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException {
        return null;
    }

    @Override
    public void invokeAsync(Channel channel, RpcCommand request, long timeoutMillis, InvokeCallback callback)
            throws InterruptedException, RpcTooMuchRequestException, RpcSendRequestException {

    }

    @Override
    public void invokeOneWay(Channel channel, RpcCommand request, long timeoutMillis)
            throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException {

    }
}
