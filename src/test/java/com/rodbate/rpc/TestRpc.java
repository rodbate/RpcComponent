package com.rodbate.rpc;


import com.google.gson.Gson;
import com.rodbate.rpc.common.CommonUtil;
import com.rodbate.rpc.common.FieldNotNull;
import com.rodbate.rpc.exception.*;
import com.rodbate.rpc.netty.*;
import com.rodbate.rpc.protocol.CommandCustomHeader;
import com.rodbate.rpc.protocol.RpcCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.nio.ByteBuffer;


public class TestRpc {


    private final static Gson gson = new Gson();

    public static NettyRpcServer createServer(String hostName, int port)
    {

        NettyServerConfig config = new NettyServerConfig();
        config.setHostName(hostName);
        config.setListenPort(port);


        NettyRpcServer server = new NettyRpcServer(config, new ChannelEventListener() {
            @Override
            public void onChannelConnect(String remoteAddress, Channel channel) {
                System.out.println("connect " + remoteAddress);
            }

            @Override
            public void onChannelClose(String remoteAddress, Channel channel) {
                System.out.println("close " + remoteAddress);
            }

            @Override
            public void onChannelException(String remoteAddress, Channel channel) {

            }

            @Override
            public void onChannelIdle(String remoteAddress, Channel channel) {

            }
        });


        server.start();

        return server;
    }

    public NettyRpcClient createClient()
    {
        NettyClientConfig config = new NettyClientConfig();

        NettyRpcClient client = new NettyRpcClient(config, new ChannelEventListener() {
            @Override
            public void onChannelConnect(String remoteAddress, Channel channel) {
                System.out.println(" client connect " + remoteAddress);
            }

            @Override
            public void onChannelClose(String remoteAddress, Channel channel) {
                System.out.println("client close " + remoteAddress);
            }

            @Override
            public void onChannelException(String remoteAddress, Channel channel) {

            }

            @Override
            public void onChannelIdle(String remoteAddress, Channel channel) {

            }
        });
        client.start();

        return client;
    }



    @Test
    public void testInvokeSync() throws InterruptedException, RpcSendRequestException, RpcConnectException, RpcTimeoutException {
        NettyRpcServer server = createServer("127.0.0.1", 8888);
        RpcClient client = createClient();

        server.registerProcessor(0, new NettyRpcRequestProcessor() {
            private int i = 1;
            @Override
            public RpcCommand processRequest(ChannelHandlerContext ctx, RpcCommand request) throws Exception {

                System.out.println(" === receive request : " + gson.toJson(request));
                RpcCommand responseCommand = RpcCommand.createResponseCommand(request.getCode(), "response-" + i++);
                responseCommand.setBody(request.getBody());
                return responseCommand;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, null);


        for (int i = 0; i < 100; i++) {
            RpcCommand request = RpcCommand.createRequestCommand(0, "request-" + i + 1);
            request.setBody(("  body " + (i + 1)).getBytes() );

            RpcCommand command = client.invokeSync("localhost:8888", request);

            System.out.println(("============ response header : " + new Gson().toJson(command)));
            System.out.println("=========== response body : " + new String(command.getBody()));
        }

    }


    @Test
    public void testInvokeAsync() throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException, RpcConnectException {

        NettyRpcServer server = createServer("127.0.0.1", 8888);
        RpcClient client = createClient();

        server.registerProcessor(0, new NettyRpcRequestProcessor() {
            private int i = 1;
            @Override
            public RpcCommand processRequest(ChannelHandlerContext ctx, RpcCommand request) throws Exception {

                System.out.println(" === receive request : " + gson.toJson(request));
                RpcCommand responseCommand = RpcCommand.createResponseCommand(request.getCode(), "response-" + i++);
                responseCommand.setBody(request.getBody());
                return responseCommand;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, null);




        for (int i = 0; i < 1; i++) {
            RpcCommand request = RpcCommand.createRequestCommand(0, "request-" + (1 + i));
            request.setBody(("  body " + (1 + i)).getBytes() );
            client.invokeAsync("localhost:8888", request, 3000, responseFuture -> {

                RpcCommand responseCmd = responseFuture.getResponseCmd();


                System.out.println(("============ response header : " + new Gson().toJson(responseCmd)));
                System.out.println("=========== response body : " + new String(responseCmd.getBody()));

            });
        }



        Thread.sleep(3000);
    }



    @Test
    public void testInvokeOneWay() throws InterruptedException, RpcSendRequestException, RpcTooMuchRequestException, RpcConnectException {

        NettyRpcServer server = createServer("127.0.0.1", 8888);
        RpcClient client = createClient();

        server.registerProcessor(0, new NettyRpcRequestProcessor() {
            private int i = 1;
            @Override
            public RpcCommand processRequest(ChannelHandlerContext ctx, RpcCommand request) throws Exception {

                System.out.println(" === receive request : " + gson.toJson(request));
                RpcCommand responseCommand = RpcCommand.createResponseCommand(request.getCode(), "response-" + i++);
                responseCommand.setBody(request.getBody());
                return responseCommand;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, null);


        for (int i = 0; i < 1000; i++) {
            RpcCommand request = RpcCommand.createRequestCommand(0, "request-" + (1 + i));
            request.setBody(("  body " + (1 + i)).getBytes() );

            client.invokeOneWay("localhost:8888", request, 3000);
        }

        Thread.sleep(2000);
    }


    @Test
    public void testCustomHeader() throws InterruptedException, RpcSendRequestException, RpcConnectException, RpcTimeoutException {

        NettyRpcServer server = createServer("127.0.0.1", 8888);
        RpcClient client = createClient();

        server.registerProcessor(0, new NettyRpcRequestProcessor() {
            private int i = 1;
            @Override
            public RpcCommand processRequest(ChannelHandlerContext ctx, RpcCommand request) throws Exception {

                System.out.println(" before decode header  === receive request : " + gson.toJson(request));
                request.decodeCustomHeader(TestRequestHeader.class);
                System.out.println(" after decode header  === receive request : " + gson.toJson(request.getCommandCustomHeader()));
                RpcCommand responseCommand = RpcCommand.createResponseCommand(request.getCode(), "response-" + i++);
                responseCommand.setBody(request.getBody());
                return responseCommand;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, null);


        RpcCommand request = RpcCommand.createRequestCommand(0, "request-" + (1));
        request.setCommandCustomHeader(new TestRequestHeader(111, 12.11));

        RpcCommand response = client.invokeSync("localhost:8888", request, 30000);

        System.out.println("   response " + gson.toJson(response));


    }

    public static class TestRequestHeader implements CommandCustomHeader{

        private int code;

        @FieldNotNull
        private String content;

        private double pp;

        public TestRequestHeader() {
        }

        public TestRequestHeader(int code, double pp) {
            this.code = code;
            this.pp = pp;
        }

        public TestRequestHeader(int code, String content, double pp) {
            this.code = code;
            this.content = content;
            this.pp = pp;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }

        @Override
        public void checkFields() throws RpcCommandException {
            System.out.println(" ============== check fields    ");
        }
    }


    public static class TestResponseHeader implements CommandCustomHeader {

        private int code;

        private String content;

        private double pp;

        public TestResponseHeader() {
        }

        public TestResponseHeader(int code, String content, double pp) {
            this.code = code;
            this.content = content;
            this.pp = pp;
        }

        @Override
        public String toString() {
            return gson.toJson(this);
        }

        @Override
        public void checkFields() throws RpcCommandException {
            System.out.println(" ============== check fields    ");
        }
    }




    @Test
    public void testProtocol() throws Exception {
        RpcCommand request = RpcCommand.createRequestCommand(0, "request ");

        request.setCommandCustomHeader(new TestRequestHeader(111, "adfs", 111.2));

        request.decodeCustomHeader(TestRequestHeader.class);
        System.out.println();
    }


    @Test
    public void test() {

        NettyRpcClient client = createClient();

        Bootstrap bootstrap = client.getBootstrap();

        ChannelFuture future = bootstrap.connect(CommonUtil.stringAddressToSocketAddress("127.0.0.1:8888"));

        future.awaitUninterruptibly();

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("=   connect remote channel :: " + future.channel().remoteAddress());
                System.out.printf("   state : " + future.isSuccess());
            }
        });

    }

    public static void main(String[] args) {

        NettyRpcServer server = createServer("127.0.0.1", 8888);

        server.registerProcessor(0, new NettyRpcRequestProcessor() {
            @Override
            public RpcCommand processRequest(ChannelHandlerContext ctx, RpcCommand request) throws Exception {
                System.out.println(" === receive request : " + gson.toJson(request));
                return RpcCommand.createResponseCommand(request.getCode(), "response ");
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, null);
    }



    @Test
    public void testConnect()
    {

        Bootstrap b = new Bootstrap();

        b.group(new NioEventLoopGroup()).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<SocketChannel>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SocketChannel msg) throws Exception {
                                System.out.println(" ======= " + msg.remoteAddress());
                            }
                        });
                    }
                });


        ChannelFuture future = b.connect(CommonUtil.stringAddressToSocketAddress("127.0.0.1:8888"));


        future.awaitUninterruptibly();

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("=   connect remote channel :: " + future.channel().remoteAddress());
            }
        });


    }


}


