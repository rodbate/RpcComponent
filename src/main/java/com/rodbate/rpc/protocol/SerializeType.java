package com.rodbate.rpc.protocol;



/**
 *
 *
 * rpc通信协议类型
 *
 */

public enum SerializeType {


    /** json 协议 */
    JSON((byte)0),


    /** rpc内置协议 */
    RBRPC((byte)1);



    private final byte code;


    SerializeType(byte code) {
        this.code = code;
    }

    public static SerializeType valueOf(byte b)
    {
        switch (b)
        {

            case (byte) 0 : return JSON;

            case (byte) 1 : return RBRPC;

            default: throw new RuntimeException("No such serialize type");
        }
    }

    public byte getCode() {
        return code;
    }
}
