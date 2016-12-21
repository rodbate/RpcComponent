package com.rodbate.rpc;


import com.rodbate.rpc.exception.RpcConnectException;
import com.rodbate.rpc.exception.RpcSendRequestException;
import com.rodbate.rpc.exception.RpcTimeoutException;
import com.rodbate.rpc.exception.RpcTooMuchRequestException;
import com.rodbate.rpc.netty.InvokeCallback;
import com.rodbate.rpc.protocol.RpcCommand;

import java.util.List;

public interface RpcClient extends RpcService {



    boolean isChannelWritable(final String address);


    //127.0.0.1:8888
    List<String> getNameServerAddressList();


    void updateNameServerAddressList(final List<String> list);


    RpcCommand invokeSync(final String address, final RpcCommand request)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException, RpcConnectException;


    RpcCommand invokeSync(final String address, final RpcCommand request, final long timeoutMillis)
            throws InterruptedException, RpcTimeoutException, RpcSendRequestException, RpcConnectException;


    void invokeAsync(final String address, final RpcCommand request, final long timeoutMillis, final InvokeCallback callback)
            throws InterruptedException, RpcTooMuchRequestException, RpcSendRequestException, RpcConnectException;



    void invokeOneWay(final String address, final RpcCommand request, final long timeoutMillis)
            throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException, RpcConnectException;


}
