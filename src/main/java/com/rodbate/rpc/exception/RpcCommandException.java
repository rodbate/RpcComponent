package com.rodbate.rpc.exception;




public class RpcCommandException extends Exception {


    public RpcCommandException(String message) {
        super(message);
    }

    public RpcCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
