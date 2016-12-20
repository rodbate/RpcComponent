package com.rodbate.rpc;


import com.rodbate.rpc.netty.NettyRpcRequestProcessor;

import java.util.concurrent.ExecutorService;



public interface RpcServer extends RpcService {


    int localListenPort();

    void registerDefaultProcessor(final NettyRpcRequestProcessor processor, final ExecutorService service);

}
