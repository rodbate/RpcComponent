package com.rodbate.rpc.netty;


import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.channel.ChannelHandlerContext;


public interface NettyRpcRequestProcessor {


    RpcCommand processRequest(ChannelHandlerContext ctx, RpcCommand request) throws Exception;

    boolean rejectRequest();
}
