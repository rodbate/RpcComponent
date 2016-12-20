package com.rodbate.rpc.protocol;


import com.rodbate.rpc.common.RpcCommandHelper;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;



import static com.rodbate.rpc.common.CommonUtil.*;
import static com.rodbate.rpc.common.RpcCommandHelper.*;

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



    public static byte[] encoderHeader(final RpcCommand cmd)
    {

        int headerLength = 0;

        //code int
        headerLength += 4;

        //flag byte
        headerLength += 1;

        //seq int
        headerLength += 4;

        //language byte
        headerLength += 1;

        //remark length short
        headerLength += 2;
        short remarkLength = 0;
        byte[] remarkData = null;
        if (isNotEmpty(cmd.getRemark()))
        {
            remarkData = cmd.getRemark().getBytes(CHARSET_UTF8);
            if (remarkData.length > Short.MAX_VALUE)
            {
                throw new IllegalArgumentException("remark length cannot be more than short");
            }
            remarkLength = (short) remarkData.length;
        }
        headerLength += remarkLength;

        //ext fields short
        headerLength += 2;

        byte[] extFieldsData = mapSerialize(cmd);
        if (extFieldsData != null) {
            headerLength += extFieldsData.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(headerLength);

        //code int
        buffer.putInt(cmd.getCode());

        //flag byte
        buffer.put((byte) cmd.getFlag());

        //seq int
        buffer.putInt(cmd.getSeq());

        //language byte
        buffer.put(cmd.getLanguage());

        //remark length short
        buffer.putShort(remarkLength);

        //remark data
        if (remarkLength > 0)
        {
            buffer.put(remarkData);
        }

        //ext fields length short
        buffer.putShort((short) (extFieldsData == null ? 0 : extFieldsData.length));

        if (extFieldsData != null)
        {
            buffer.put(extFieldsData);
        }

        return buffer.array();
    }


    public static byte[] mapSerialize(final RpcCommand cmd)
    {

        Map<String, String> extFields = cmd.getExtFields();

        if (extFields == null || extFields.isEmpty())
        {
            return null;
        }

        short totalLength = 0;

        //keyLength[byte]->keyData[byte[]]->valueLength[short]->valueData[byte[]]

        for (Map.Entry<String, String> entry : extFields.entrySet())
        {
            byte[] key = entry.getKey().getBytes(CHARSET_UTF8);
            totalLength += 1 + key.length;

            byte[] value = entry.getValue().getBytes(CHARSET_UTF8);
            totalLength += 2 + value.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        for (Map.Entry<String, String> entry : extFields.entrySet())
        {
            byte[] key = entry.getKey().getBytes(CHARSET_UTF8);
            buffer.put((byte) key.length);
            buffer.put(key);

            byte[] value = entry.getValue().getBytes(CHARSET_UTF8);
            buffer.putShort((short) value.length);
            buffer.put(value);
        }


        return buffer.array();
    }

}
