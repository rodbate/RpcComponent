package com.rodbate.rpc.protocol;


import com.google.gson.Gson;
import com.rodbate.rpc.common.RpcCommandHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RpcCommandSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcCommandSerializable.class);

    private static final Gson GSON = new Gson();


    public static <T> T fromJson(byte[] data, Class<T> clazz)
    {

        Objects.requireNonNull(data);
        Objects.requireNonNull(clazz);


        return GSON.fromJson(new String(data, RpcCommandHelper.CHARSET_UTF8), clazz);
    }


    public static String toJson(Object obj)
    {

        Objects.requireNonNull(obj);

        return GSON.toJson(obj);
    }

    public static byte[] encode(Object obj)
    {
        return toJson(obj).getBytes(RpcCommandHelper.CHARSET_UTF8);
    }


}
