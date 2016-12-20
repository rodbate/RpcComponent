package com.rodbate.rpc.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public abstract class ServiceThread implements Runnable {


    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceThread.class);

    //join time 90s
    private final static long JOIN_TIME = 90 * 1000;
    private final Thread thread;

    private volatile boolean hasNotified = false;

    private volatile boolean stopped = false;

    public ServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }


    public void start()
    {
        this.thread.start();
    }


    public void shutdown()
    {
        shutdown(false);
    }

    public void shutdown(boolean interrupt)
    {
        this.stopped = true;
        LOGGER.info(" shutdown service thread {}, interrupt : {}", this.getServiceName(), interrupt);

        synchronized (this)
        {
            if (!hasNotified)
            {
                this.hasNotified = true;
                this.notify();
            }

        }

        try {

            if (interrupt)
            {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();

            this.thread.join(JOIN_TIME);

            long waitTime = System.currentTimeMillis() - beginTime;

            LOGGER.info("  service thread {} join time(ms) : [{}] , wait time(ms) : [{}]",
                    this.getServiceName(), JOIN_TIME, waitTime);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop()
    {
        stop(false);
    }

    public void setDaemon(final boolean daemon)
    {
        this.thread.setDaemon(daemon);
    }

    public void stop(boolean interrupt)
    {
        this.stopped = true;

        LOGGER.info("stop service thread {}  interrupt :{}", this.getServiceName(), interrupt);
        synchronized (this)
        {
            if (!hasNotified)
            {
                this.hasNotified = true;
                this.notify();
            }
        }

        if (interrupt)
        {
            this.thread.interrupt();
        }

    }

    public void wakeUp()
    {
        synchronized (this)
        {
            if (!hasNotified)
            {
                this.hasNotified = true;
                this.notify();
            }
        }
    }

    public void waitForRunning(long time)
    {
        synchronized (this)
        {
            if (hasNotified)
            {
                this.hasNotified = false;
                this.onWaitEnd();
                return;
            }

            try {
                this.wait(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.hasNotified = false;
                this.onWaitEnd();
            }

        }

    }


    public boolean isStopped() {
        return stopped;
    }

    public abstract String getServiceName();

    protected void onWaitEnd(){};
}
