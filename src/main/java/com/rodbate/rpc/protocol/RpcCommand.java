package com.rodbate.rpc.protocol;


import com.google.gson.Gson;

import java.nio.ByteBuffer;
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



    public ByteBuffer encode()
    {

        //total length
        int totalLength = 4;

        //serialize type + header length
        totalLength += 4;

        byte[] headerData = encodeHeader();

        totalLength += headerData.length;

        if (this.body != null)
        {
            totalLength += this.body.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        //serialize type + header length
        byte[] serializeAndHeaderLength = getSerializeAndHeaderLength(headerData.length);

        buffer.putInt(totalLength);

        buffer.put(serializeAndHeaderLength);

        buffer.put(headerData);

        if (this.body != null)
        {
            buffer.put(this.body);
        }

        buffer.flip();

        return buffer;
    }

    private byte[] getSerializeAndHeaderLength(int headerLength)
    {

        byte[] ths = new byte[4];

        ths[0] = this.rpcSerializableType;
        ths[1] = (byte) ((headerLength >>> 16) & 0xFF);
        ths[2] = (byte) ((headerLength >>> 8) & 0xFF);
        ths[3] = (byte) (headerLength & 0xFF);

        return ths;
    }


    private byte[] encodeHeader()
    {

        SerializeType serializeType = SerializeType.valueOf(this.rpcSerializableType);

        if (serializeType == SerializeType.JSON)
        {
            return RpcCommandSerializable.encode(this);
        }
        else
        {
            return RBRpcSerializable.encoderHeader(this);
        }

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

    public RpcCommandType getCmdType()
    {
        if (isRpcResponse())
        {
            return RpcCommandType.RESPONSE_COMMAND;
        }

        return RpcCommandType.REQUEST_COMMAND;
    }


    public static RpcCommand createResponseCommand(int code, String remark)
    {
        return createResponseCommand(code, remark, null);
    }

    public static RpcCommand createResponseCommand(int code, String remark, Class<? extends CommandCustomHeader> header)
    {
        RpcCommand cmd = new RpcCommand();
        cmd.setCode(code);
        cmd.setRemark(remark);
        cmd.markRpcResponse();

        if (header != null)
        {
            try {
                CommandCustomHeader commandCustomHeader = header.newInstance();
                cmd.setCommandCustomHeader(commandCustomHeader);
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        return cmd;
    }


    public static void main(String[] args) {

        RpcCommand cmd = new RpcCommand();
        cmd.setCode(1);
        cmd.setRemark("test remark");
        //cmd.setRpcSerializableType(SerializeType.RBRPC.getCode());
        cmd.setBody("request".getBytes());
        System.out.println("before " + new Gson().toJson(cmd));

        ByteBuffer buffer = cmd.encode();

        System.out.println(new Gson().toJson(buffer.array()));

        buffer.getInt();

        ByteBuffer slice = buffer.slice();

        System.out.println(new Gson().toJson(slice.array()));

        System.out.println("decode" + new Gson().toJson(decode(slice)));

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
