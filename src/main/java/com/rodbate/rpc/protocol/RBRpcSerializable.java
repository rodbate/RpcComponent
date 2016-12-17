package com.rodbate.rpc.protocol;


import com.rodbate.rpc.common.RpcCommandHelper;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 *
 *
 *
 */
public class RBRpcSerializable {





    public static RpcCommand decodeHeader(final byte[] data)
    {

        Objects.requireNonNull(data);

        RpcCommand cmd = new RpcCommand();

        ByteBuffer buffer = ByteBuffer.wrap(data);

        //<code> int
        int code = buffer.getInt();
        cmd.setCode(code);

        //<flag> byte
        byte flag = buffer.get();
        cmd.setFlag(flag);

        //<seq> int
        int seq = buffer.getInt();
        cmd.setSeq(seq);

        //<language> byte
        byte language = buffer.get();
        cmd.setLanguage(language);

        //<remark length> short
        short remarkLength = buffer.getShort();
        if (remarkLength > 0)
        {
            //<remark data>
            byte remark[] = new byte[remarkLength];
            buffer.get(remark);
            cmd.setRemark(new String(remark, RpcCommandHelper.CHARSET_UTF8));
        }

        //<ext field length> short
        short extFieldLength = buffer.getShort();
        if (extFieldLength > 0)
        {
            //<ext fields data>
            byte[] extFields = new byte[extFieldLength];
            buffer.get(extFields);
            cmd.setExtFields(mapDeserialize(data));
        }

        return cmd;
    }


    //反序列化map
    private static Map<String, String> mapDeserialize(final byte[] data)
    {

        Map<String, String> map = new HashMap<>();

        ByteBuffer buffer = ByteBuffer.wrap(data);

        //keyLength[byte]->keyData[byte[]]->valueLength[short]->valueData[byte[]]
        byte keyLen;
        byte[] keyData;
        byte valueLen;
        byte[] valueData;

        while (buffer.hasRemaining())
        {
            //key
            keyLen = buffer.get();
            keyData = new byte[keyLen];
            buffer.get(keyData);

            //value
            valueLen = buffer.get();
            valueData = new byte[valueLen];
            buffer.get(valueData);

            map.put(new String(keyData, RpcCommandHelper.CHARSET_UTF8),
                    new String(valueData, RpcCommandHelper.CHARSET_UTF8));

        }

        return map;
    }

}
