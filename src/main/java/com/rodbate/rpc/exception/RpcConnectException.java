package com.rodbate.rpc.exception;




public class RpcConnectException extends Exception {


    public RpcConnectException(String address) {
        super("connect remote address [" + address + "] exception ");
    }

    public RpcConnectException(String address, Throwable cause) {
        super("connect remote address [" + address + "] exception", cause);
    }
}
