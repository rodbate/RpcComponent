package com.rodbate.rpc.netty;


import com.rodbate.rpc.common.CommonUtil;
import com.rodbate.rpc.common.RpcCommandHelper;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;


/**
 *
 *
 *
 */
public class RpcNettyDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcNettyDecoder.class);

    //16M
    private static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024;


    public RpcNettyDecoder() {
        super(MAX_FRAME_LENGTH, 0, 4, 0, 4);
    }


    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        ByteBuf frame = null;

        try {
            frame = (ByteBuf) super.decode(ctx, in);

            if (frame == null) {
                return null;
            }

            ByteBuffer byteBuffer = frame.nioBuffer();


            return RpcCommand.decode(byteBuffer);

        } catch (Exception e)
        {
            LOGGER.info(" decode exception  : " + CommonUtil.getRemoteAddressFromChannel(ctx.channel()));
            RpcCommandHelper.closeChannel(ctx.channel());
        }
        finally {

            if (frame != null)
            {
                frame.release();
            }
        }

        return null;
    }

}
