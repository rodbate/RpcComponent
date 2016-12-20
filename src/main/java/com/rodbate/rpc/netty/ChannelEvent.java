package com.rodbate.rpc.netty;


import io.netty.channel.Channel;





public class ChannelEvent {


    private final ChannelEventType eventType;

    private final String remoteAddress;

    private final Channel channel;



    public ChannelEvent(ChannelEventType eventType, String remoteAddress, Channel channel) {
        this.eventType = eventType;
        this.remoteAddress = remoteAddress;
        this.channel = channel;
    }

    public ChannelEventType getEventType() {
        return eventType;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "ChannelEvent{" +
                "eventType=" + eventType +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", channel=" + channel +
                '}';
    }
}
