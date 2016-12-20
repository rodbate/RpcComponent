package com.rodbate.rpc;


import io.netty.channel.Channel;



public interface ChannelEventListener {


    //链路连接时
    void onChannelConnect(final String remoteAddress, final Channel channel);

    //链路关闭时
    void onChannelClose(final String remoteAddress, final Channel channel);

    //发生异常时
    void onChannelException(final String remoteAddress, final Channel channel);

    //链路空闲时
    void onChannelIdle(final String remoteAddress, final Channel channel);
}
