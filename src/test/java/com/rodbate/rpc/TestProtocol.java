package com.rodbate.rpc;


import com.rodbate.rpc.protocol.RpcCommand;
import org.junit.Test;

public class TestProtocol {



    @Test
    public void test()
    {

        RpcCommand command = RpcCommand.createResponseCommand(1, "test");
    }
}
