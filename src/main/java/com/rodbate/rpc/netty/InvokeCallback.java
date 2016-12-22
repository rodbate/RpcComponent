package com.rodbate.rpc.netty;


import com.rodbate.rpc.exception.RpcCommandException;

/**
 *
 * rpc 回调
 *
 */

@FunctionalInterface
public interface InvokeCallback {

    void operationComplete(final ResponseFuture responseFuture) throws RpcCommandException;

}
