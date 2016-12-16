package com.rodbate.rpc.common;


import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

public class CommonUtil {




    public static String getRemoteAddressFromChannel(Channel channel)
    {

        Objects.requireNonNull(channel);

        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();

        return socketAddress.getAddress().getHostAddress();
    }

    public static void main(String[] args) {



    }
}
