package com.rodbate.rpc.netty;


import com.rodbate.rpc.common.SemaphoreReleaseOnlyOnce;
import com.rodbate.rpc.exception.RpcCommandException;
import com.rodbate.rpc.protocol.RpcCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class ResponseFuture {


    private final int seq;

    private final long beginTimestamp = System.currentTimeMillis();

    private final long timeoutMillis;

    private final InvokeCallback callback;

    private final CountDownLatch signal = new CountDownLatch(1);

    private final SemaphoreReleaseOnlyOnce once;


    private final AtomicBoolean executeCallbackOnce = new AtomicBoolean(false);

    private volatile RpcCommand responseCmd;

    private volatile Throwable cause;

    private volatile boolean sendRequestOk = true;

    public ResponseFuture(int seq,
                          long timeoutMillis,
                          InvokeCallback callback,
                          SemaphoreReleaseOnlyOnce once) {
        this.seq = seq;
        this.timeoutMillis = timeoutMillis;
        this.callback = callback;
        this.once = once;
    }


    public void executeCallback() throws RpcCommandException
    {
        if (callback != null && executeCallbackOnce.compareAndSet(false, true))
        {
            callback.operationComplete(this);
        }
    }


    public void release()
    {
        if (once != null)
        {
            once.release();
        }
    }

    public boolean isTimeout()
    {
        long diff = System.currentTimeMillis() - beginTimestamp;
        return diff > timeoutMillis;
    }

    public RpcCommand waitForResponse() throws InterruptedException
    {
        signal.await();
        return this.responseCmd;
    }

    public RpcCommand waitForResponse(final long timeoutMillis) throws InterruptedException
    {
        signal.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCmd;
    }

    public void putResponse(final RpcCommand responseCmd)
    {
        this.responseCmd = responseCmd;
        signal.countDown();
    }


    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getSeq() {
        return seq;
    }

    public InvokeCallback getCallback() {
        return callback;
    }

    public SemaphoreReleaseOnlyOnce getOnce() {
        return once;
    }

    public RpcCommand getResponseCmd() {
        return responseCmd;
    }

    public void setResponseCmd(RpcCommand responseCmd) {
        this.responseCmd = responseCmd;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public boolean isSendRequestOk() {
        return sendRequestOk;
    }

    public void setSendRequestOk(boolean sendRequestOk) {
        this.sendRequestOk = sendRequestOk;
    }

    @Override
    public String toString() {
        return "ResponseFuture{" +
                "seq=" + seq +
                ", beginTimestamp=" + beginTimestamp +
                ", timeoutMillis=" + timeoutMillis +
                ", callback=" + callback +
                ", signal=" + signal +
                ", once=" + once +
                ", executeCallbackOnce=" + executeCallbackOnce +
                ", responseCmd=" + responseCmd +
                ", cause=" + cause +
                ", sendRequestOk=" + sendRequestOk +
                '}';
    }
}
