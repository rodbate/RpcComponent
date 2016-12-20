package com.rodbate.rpc;







public interface RpcClient extends RpcService {



    boolean isChannelWritable(final String address);


}
