package com.rodbate.rpc.common;


import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;



public class SemaphoreReleaseOnlyOnce {


    private final AtomicBoolean releaseOnce = new AtomicBoolean(false);


    private final Semaphore semaphore;


    public SemaphoreReleaseOnlyOnce(Semaphore semaphore) {
        this.semaphore = semaphore;
    }


    public void release()
    {
        if (semaphore != null && releaseOnce.compareAndSet(false, true))
        {
            semaphore.release();
        }
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }
}
