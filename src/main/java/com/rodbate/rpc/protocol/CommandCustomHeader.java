package com.rodbate.rpc.protocol;


import com.rodbate.rpc.exception.RpcCommandException;

/**
 *
 * cmd 自定义头
 *
 */
public interface CommandCustomHeader {


    void checkFields() throws RpcCommandException;

}
