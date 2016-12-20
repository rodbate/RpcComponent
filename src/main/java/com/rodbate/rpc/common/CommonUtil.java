package com.rodbate.rpc.common;


import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Objects;

public class CommonUtil {




    public static String getRemoteAddressFromChannel(Channel channel)
    {

        Objects.requireNonNull(channel);

        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();

        return socketAddress.getAddress().getHostAddress();
    }


    public static boolean isNotEmpty(final String src)
    {
        return (src != null) && (src.trim().length() > 0);
    }

    public static boolean isEmpty(final String src)
    {
        return !isNotEmpty(src);
    }


    public static void main(String[] args) {



    }
}
