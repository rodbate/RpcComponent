package com.rodbate.rpc.protocol;



/**
 *
 *
 *
 * 编程语言类型
 *
 */

public enum Language {


    JAVA((byte)0);


    private final byte code;

    Language(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static Language valueOf(byte b)
    {
        switch (b)
        {
            case 0 : return JAVA;

            default: throw new RuntimeException("No such language type");
        }
    }
}
