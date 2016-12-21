package com.rodbate.rpc.netty;





public class NettyClientConfig {


    private int clientSelectorThreadNums = 4;

    private int clientWorkerThreadNums = 4;

    private int clientCallbackThreadNums = Runtime.getRuntime().availableProcessors();


    private int clientSemaphoreAsyncValue = 256;
    private int clientSemaphoreOneWayValue = 64;

    private int connectTimeoutMillis = 3000;

    private int clientChannelIdleTimeSeconds = 120;

    private int clientSocketSndBufSize = 65535;
    private int clientSocketRcvBufSize = 65535;
    private boolean clientPooledByteBufAllocatorEnable = false;


    private boolean clientCloseSocketIfTimeout = false;

    public int getClientSelectorThreadNums() {
        return clientSelectorThreadNums;
    }

    public void setClientSelectorThreadNums(int clientSelectorThreadNums) {
        this.clientSelectorThreadNums = clientSelectorThreadNums;
    }

    public int getClientWorkerThreadNums() {
        return clientWorkerThreadNums;
    }

    public void setClientWorkerThreadNums(int clientWorkerThreadNums) {
        this.clientWorkerThreadNums = clientWorkerThreadNums;
    }

    public int getClientCallbackThreadNums() {
        return clientCallbackThreadNums;
    }

    public void setClientCallbackThreadNums(int clientCallbackThreadNums) {
        this.clientCallbackThreadNums = clientCallbackThreadNums;
    }

    public int getClientSemaphoreAsyncValue() {
        return clientSemaphoreAsyncValue;
    }

    public void setClientSemaphoreAsyncValue(int clientSemaphoreAsyncValue) {
        this.clientSemaphoreAsyncValue = clientSemaphoreAsyncValue;
    }

    public int getClientSemaphoreOneWayValue() {
        return clientSemaphoreOneWayValue;
    }

    public void setClientSemaphoreOneWayValue(int clientSemaphoreOneWayValue) {
        this.clientSemaphoreOneWayValue = clientSemaphoreOneWayValue;
    }

    public int getClientChannelIdleTimeSeconds() {
        return clientChannelIdleTimeSeconds;
    }

    public void setClientChannelIdleTimeSeconds(int clientChannelIdleTimeSeconds) {
        this.clientChannelIdleTimeSeconds = clientChannelIdleTimeSeconds;
    }

    public int getClientSocketSndBufSize() {
        return clientSocketSndBufSize;
    }

    public void setClientSocketSndBufSize(int clientSocketSndBufSize) {
        this.clientSocketSndBufSize = clientSocketSndBufSize;
    }

    public int getClientSocketRcvBufSize() {
        return clientSocketRcvBufSize;
    }

    public void setClientSocketRcvBufSize(int clientSocketRcvBufSize) {
        this.clientSocketRcvBufSize = clientSocketRcvBufSize;
    }

    public boolean isClientPooledByteBufAllocatorEnable() {
        return clientPooledByteBufAllocatorEnable;
    }

    public void setClientPooledByteBufAllocatorEnable(boolean clientPooledByteBufAllocatorEnable) {
        this.clientPooledByteBufAllocatorEnable = clientPooledByteBufAllocatorEnable;
    }

    public boolean isClientCloseSocketIfTimeout() {
        return clientCloseSocketIfTimeout;
    }

    public void setClientCloseSocketIfTimeout(boolean clientCloseSocketIfTimeout) {
        this.clientCloseSocketIfTimeout = clientCloseSocketIfTimeout;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }
}
