package com.rodbate.rpc.netty;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;




public class DefaultThreadFactory implements ThreadFactory {


    private final AtomicInteger index = new AtomicInteger(0);

    private final String threadPrefixName;

    public DefaultThreadFactory(String threadPrefixName) {
        this.threadPrefixName = threadPrefixName;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, threadPrefixName + "-" + Math.abs(index.incrementAndGet()));
    }
}
