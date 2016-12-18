package com.rodbate.rpc.netty;


import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;




public class RpcNettyEncoder extends MessageToByteEncoder<RpcCommand>{



    @Override
    protected void encode(ChannelHandlerContext ctx, RpcCommand command, ByteBuf out) throws Exception {





    }


}
