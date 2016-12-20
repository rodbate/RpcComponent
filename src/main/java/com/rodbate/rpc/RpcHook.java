package com.rodbate.rpc;


import com.rodbate.rpc.protocol.RpcCommand;



public interface RpcHook {


    void doBeforeRequest(final String remoteAddress, final RpcCommand request);


    void doAfterResponse(final String remoteAddress, final RpcCommand request, final RpcCommand response);

}
