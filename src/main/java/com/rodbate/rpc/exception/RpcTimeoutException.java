package com.rodbate.rpc.exception;




public class RpcTimeoutException extends Exception {


    public RpcTimeoutException(String message) {
        super(message);
    }

    public RpcTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcTimeoutException(Throwable cause) {
        super(cause);
    }

    public RpcTimeoutException(String address, long timeoutMillis, Throwable cause)
    {
        super("wait response on channel <" + address + "> timeout " + timeoutMillis + "(ms)", cause);
    }
}
