package com.rodbate.rpc.netty;








public class NettyServerConfig {


    private String hostName = "localhost";
    private int listenPort = 8888;
    private int serverWorkThreads = 8;
    private int serverCallbackExecutorThreads = 0;
    private int serverSelectorThreads = 4;
    private int serverSemaphoreAsyncValue = 256;
    private int serverSemaphoreOneWayValue = 64;

    private int serverChannelIdleTimeSeconds = 120;

    private int serverSocketSndBufSize = 65535;
    private int serverSocketRcvBufSize = 65535;
    private boolean serverPooledByteBufAllocatorEnable = true;


    private boolean userEPollNativeSelector = false;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getServerWorkThreads() {
        return serverWorkThreads;
    }

    public void setServerWorkThreads(int serverWorkThreads) {
        this.serverWorkThreads = serverWorkThreads;
    }

    public int getServerCallbackExecutorThreads() {
        return serverCallbackExecutorThreads;
    }

    public void setServerCallbackExecutorThreads(int serverCallbackExecutorThreads) {
        this.serverCallbackExecutorThreads = serverCallbackExecutorThreads;
    }

    public int getServerSelectorThreads() {
        return serverSelectorThreads;
    }

    public void setServerSelectorThreads(int serverSelectorThreads) {
        this.serverSelectorThreads = serverSelectorThreads;
    }

    public int getServerSemaphoreAsyncValue() {
        return serverSemaphoreAsyncValue;
    }

    public void setServerSemaphoreAsyncValue(int serverSemaphoreAsyncValue) {
        this.serverSemaphoreAsyncValue = serverSemaphoreAsyncValue;
    }

    public int getServerSemaphoreOneWayValue() {
        return serverSemaphoreOneWayValue;
    }

    public void setServerSemaphoreOneWayValue(int serverSemaphoreOneWayValue) {
        this.serverSemaphoreOneWayValue = serverSemaphoreOneWayValue;
    }

    public int getServerChannelIdleTimeSeconds() {
        return serverChannelIdleTimeSeconds;
    }

    public void setServerChannelIdleTimeSeconds(int serverChannelIdleTimeSeconds) {
        this.serverChannelIdleTimeSeconds = serverChannelIdleTimeSeconds;
    }

    public int getServerSocketSndBufSize() {
        return serverSocketSndBufSize;
    }

    public void setServerSocketSndBufSize(int serverSocketSndBufSize) {
        this.serverSocketSndBufSize = serverSocketSndBufSize;
    }

    public int getServerSocketRcvBufSize() {
        return serverSocketRcvBufSize;
    }

    public void setServerSocketRcvBufSize(int serverSocketRcvBufSize) {
        this.serverSocketRcvBufSize = serverSocketRcvBufSize;
    }

    public boolean isServerPooledByteBufAllocatorEnable() {
        return serverPooledByteBufAllocatorEnable;
    }

    public void setServerPooledByteBufAllocatorEnable(boolean serverPooledByteBufAllocatorEnable) {
        this.serverPooledByteBufAllocatorEnable = serverPooledByteBufAllocatorEnable;
    }

    public boolean isUserEPollNativeSelector() {
        return userEPollNativeSelector;
    }

    public void setUserEPollNativeSelector(boolean userEPollNativeSelector) {
        this.userEPollNativeSelector = userEPollNativeSelector;
    }
}
