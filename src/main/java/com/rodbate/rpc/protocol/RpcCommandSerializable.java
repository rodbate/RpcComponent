package com.rodbate.rpc.protocol;


import com.google.gson.Gson;
import com.rodbate.rpc.common.RpcCommandHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

public class RpcCommandSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcCommandSerializable.class);


    public static <T> T fromJson(byte[] data, Class<T> clazz)
    {

        Objects.requireNonNull(data);
        Objects.requireNonNull(clazz);

        Gson gson = new Gson();


        return gson.fromJson(new String(data, RpcCommandHelper.CHARSET_UTF8), clazz);
    }


    public static String toJson(Object obj)
    {

        Objects.requireNonNull(obj);

        Gson gson = new Gson();

        return gson.toJson(obj);
    }


}
