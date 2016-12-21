package com.rodbate.rpc;


import com.rodbate.rpc.exception.RpcSendRequestException;
import com.rodbate.rpc.exception.RpcTimeoutException;
import com.rodbate.rpc.exception.RpcTooMuchRequestException;
import com.rodbate.rpc.netty.InvokeCallback;
import com.rodbate.rpc.netty.NettyRpcRequestProcessor;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

public interface RpcService {

    void start();


    void shutdown();

    void registerRpcHook(RpcHook rpcHook);


    void registerProcessor(final int requestCode, final NettyRpcRequestProcessor processor, final ExecutorService service);

    void registerDefaultProcessor(final NettyRpcRequestProcessor processor, final ExecutorService service);


}
