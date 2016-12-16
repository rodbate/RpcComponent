package com.rodbate.rpc.protocol;


/**
 *
 *
 * rpc 通讯类型
 *
 */
public enum RpcCommandType {

    //请求
    REQUEST_COMMAND((byte)0),

    //响应
    RESPONSE_COMMAND((byte)1);


    private final byte type;

    RpcCommandType(byte type) {
        this.type = type;
    }

    public static RpcCommandType valueOf(byte b)
    {
        switch (b)
        {

            case (byte) 0 : return REQUEST_COMMAND;

            case (byte) 1 : return RESPONSE_COMMAND;

            default: throw new RuntimeException("No such rpc command type");
        }
    }

    public byte getType() {
        return type;
    }

}
