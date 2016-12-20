package com.rodbate.rpc.exception;





public class RpcSendRequestException extends Exception {


    public RpcSendRequestException(String message) {
        super(message);
    }


    public RpcSendRequestException(Throwable cause) {
        super(cause);
    }

    public RpcSendRequestException(String address, Throwable cause)
    {
        super("send request to channel <" + address +"> failed ", cause);
    }

}
