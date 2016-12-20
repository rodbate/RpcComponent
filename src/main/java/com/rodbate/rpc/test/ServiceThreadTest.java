package com.rodbate.rpc.test;


import com.rodbate.rpc.common.ServiceThread;

public class ServiceThreadTest {


    public static void main(String[] args) throws InterruptedException {


        TestThread t = new TestThread();
        t.setDaemon(true);


        t.start();

        new Thread(){
            @Override
            public void run() {
                t.shutdown();
            }
        }.start();

        t.waitForRunning(100000);

        //Thread.sleep(3000);







    }


    static class TestThread extends ServiceThread {


        @Override
        public String getServiceName() {
            return getClass().getCanonicalName();
        }

        @Override
        public void run() {

            for (int i = 0; i < 10; i++) {
                System.out.println("current time (s) " + i);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        }

        @Override
        protected void onWaitEnd() {
            System.out.println("======== wait end =======");
        }
    }
}
