package com.rodbate.rpc.protocol;


import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.rodbate.rpc.exception.RpcCommandException;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * rpc 通信命令
 *
 */
public class RpcCommand {


    private static AtomicInteger requestId = new AtomicInteger(0);


    private final static SerializeType rpcSerializableTypeInThisServer = SerializeType.JSON;

    protected RpcCommand() {
    }

    //单次rpc mask
    private final static int RPC_ONE_WAY = 1;

    /**
     * 服务端处理器代号
     *
     * 协议中为int类型 占4个字节
     *
     */
    private int code;


    /**
     * rpc 类型: 请求 还是响应 还是单次rpc
     *
     * 默认 flag = 0 为请求类型
     *
     * 协议中为byte类型 占一个字节
     */
    private int flag;


    /**
     *
     * 通讯编解码协议类型
     * 协议中为byte类型 占一个字节
     */
    private byte rpcSerializableType = rpcSerializableTypeInThisServer.getCode();


    /**
     *
     * 序列号 每一对请求和响应的序列号是相同的
     * 协议中为int类型 占4个字节
     */
    private int seq = requestId.incrementAndGet();


    private byte language = Language.JAVA.getCode();


    //备注
    private String remark;

    //头部可扩展字段
    private Map<String, String> extFields;

    private transient CommandCustomHeader commandCustomHeader;

    private transient byte[] body;


    /**
     *
     * 将此rpc置为单次rpc
     */
    public void markRpcOneWay()
    {
        this.flag |= 1 << RPC_ONE_WAY;
    }


    /**
     * 判断rpc是否为单次rpc
     *
     * @return true or false
     */
    public boolean isRpcOneWay()
    {
        int oneWay = 1 << RPC_ONE_WAY;
        return (this.flag & oneWay) == oneWay;
    }

    /**
     *
     * 将此rpc设置为响应类型
     *
     */
    public void markRpcResponse()
    {
        this.flag |= 1;
    }

    /**
     * 判断该次rpc是否是响应类型
     *
     * @return true or false
     */
    public boolean isRpcResponse()
    {
        return (this.flag & 1) == 1;
    }


    //解码
    public static RpcCommand decode(final ByteBuffer buffer)
    {
        int length = buffer.limit();

        //serialize type[1个字节] + header length[3个字节]
        int serializeAndHead = buffer.getInt();

        int headerLength = getHeaderLength(serializeAndHead);
        byte headerData[] = new byte[headerLength];
        buffer.get(headerData);

        //解析头部
        RpcCommand cmd = headerDecode(headerData, getProtocolType(serializeAndHead));


        //解析body
        int bodyLength = length - 4 -headerLength;

        if (bodyLength > 0)
        {
            byte[] bodyData = new byte[bodyLength];
            buffer.get(bodyData);
            cmd.body = bodyData;
        }

        return cmd;
    }

    private static SerializeType getProtocolType(int serializeAndHead) {
        return SerializeType.valueOf((byte)((serializeAndHead >>> 24) & 0xFF));
    }


    public static RpcCommand headerDecode(byte[] data, SerializeType type)
    {

        switch (type)
        {
            case JSON:
            {
                RpcCommand cmd = RpcCommandSerializable.fromJson(data, RpcCommand.class);
                cmd.setRpcSerializableType(type.getCode());
                return cmd;
            }
            case RBRPC:
            {
                RpcCommand cmd = RBRpcSerializable.decodeHeader(data);
                cmd.setRpcSerializableType(type.getCode());
                return cmd;
            }
            default:
                throw new RuntimeException("no such serialize type");

        }
    }


    /**
     * 获取 包头的长度
     *
     * @param serializeAndHead serialize type[1个字节] + header length[3个字节]
     * @return 包头的长度
     */
    public static int getHeaderLength(final int serializeAndHead)
    {
        return serializeAndHead & 0xFFFFFF;
    }


    //

    public static void main(String[] args) {

        RpcCommand cmd = new RpcCommand();
        cmd.setRemark("re");
        cmd.setExtFields(new HashMap<>());
        cmd.setCommandCustomHeader(new CommandCustomHeader() {

            private int f;

            public int getF() {
                return f;
            }

            public void setF(int f) {
                this.f = f;
            }

            @Override
            public void checkFields() throws RpcCommandException {

            }
        });
        cmd.setBody("aaa".getBytes());

        System.out.println(new Gson().toJson(cmd));

    }




    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public byte getRpcSerializableType() {
        return rpcSerializableType;
    }

    public void setRpcSerializableType(byte rpcSerializableType) {
        this.rpcSerializableType = rpcSerializableType;
    }

    public void setLanguage(byte language) {
        this.language = language;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public byte getLanguage() {
        return language;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Map<String, String> getExtFields() {
        return extFields;
    }

    public void setExtFields(Map<String, String> extFields) {
        this.extFields = extFields;
    }

    public CommandCustomHeader getCommandCustomHeader() {
        return commandCustomHeader;
    }

    public void setCommandCustomHeader(CommandCustomHeader commandCustomHeader) {
        this.commandCustomHeader = commandCustomHeader;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
