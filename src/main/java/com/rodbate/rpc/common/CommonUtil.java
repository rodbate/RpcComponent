package com.rodbate.rpc.common;


import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

public class CommonUtil {




    //127.0.0.1:8888
    public static String getRemoteAddressFromChannel(Channel channel)
    {

        Objects.requireNonNull(channel);

        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();

        if (socketAddress != null) {
            return String.format("%s:%d", socketAddress.getHostName(), socketAddress.getPort());
        }

        return "";
    }

    public static SocketAddress stringAddressToSocketAddress(String address)
    {
        Objects.requireNonNull(address);

        String[] strings = address.split(":");

        return new InetSocketAddress(strings[0], Integer.valueOf(strings[1]));
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
