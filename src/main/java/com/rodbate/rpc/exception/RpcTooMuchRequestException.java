package com.rodbate.rpc.exception;







public class RpcTooMuchRequestException extends Exception {


    public RpcTooMuchRequestException(String message) {
        super(message);
    }

    public RpcTooMuchRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
