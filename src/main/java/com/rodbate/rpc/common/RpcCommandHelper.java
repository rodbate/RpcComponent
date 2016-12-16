package com.rodbate.rpc.common;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;


public class RpcCommandHelper {

    public final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");


    private static final Logger LOGGER = LoggerFactory.getLogger(RpcCommandHelper.class);






    public static void closeChannel(Channel channel)
    {

        final String remoteAddress = CommonUtil.getRemoteAddressFromChannel(channel);

        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {

                LOGGER.info("close channel <{}> , result : {}", remoteAddress, future.isSuccess() ? "success" : "failed");

            }
        });

    }
}
