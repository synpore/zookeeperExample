package com.peach.zk.peachLock;

import com.peach.zk.apacheLock.LockListener;
import com.peach.zk.util.ZkUtil;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

/**
 * Created by zengtao on 2016/8/19.
 */
public class Driver {

    public static void main(String[] args) throws InterruptedException {
        ZooKeeper zk = ZkUtil.connectServer();


        CountDownLatch countDownLatch=new CountDownLatch(10);
        Runnable task=new Runnable() {
            @Override
            public void run() {
               DistributedLock distributedLock = new DistributedLock();
                distributedLock.init(zk, new LockListener() {
                    @Override
                    public void lockAcquired() throws InterruptedException {
                    System.out.println(Thread.currentThread().getName()+"|| get lock, do something");
                    //Thread.sleep(10000);
                    }
                    @Override
                    public void lockReleased() {
                        System.out.println(Thread.currentThread().getName()+"|| lock released");
                    }
                });
                distributedLock.getLock();
                System.out.println(Thread.currentThread().getName()+" do Business");
                distributedLock.unLock();
                countDownLatch.countDown();

            }
        };
        for(int j=0;j<10;j++){
          Thread tt=  new Thread(task,"thread"+j);
          tt.setDaemon(true);
          tt.start();
    }
        countDownLatch.await();
    }
}
