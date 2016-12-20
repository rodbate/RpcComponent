package com.rodbate.rpc.netty;


import com.google.gson.Gson;
import com.rodbate.rpc.common.RpcCommandHelper;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;


public class RpcNettyEncoder extends MessageToByteEncoder<RpcCommand> {


    private static final Logger LOGGER = LoggerFactory.getLogger(RpcNettyEncoder.class);

    private static Gson gson = new Gson();

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcCommand command, ByteBuf out) throws Exception
    {


        try {

            ByteBuffer buffer = command.encode();

            out.writeBytes(buffer.array());

        }catch (Exception e)
        {
            LOGGER.info(" encode exception : ", e);
            if (command != null)
            {
                LOGGER.info(" encode rpc command : {}", gson.toJson(command));
            }
            RpcCommandHelper.closeChannel(ctx.channel());
        }


    }


}
