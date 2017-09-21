package com.peach.zk.peachLock;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.peach.zk.apacheLock.LockListener;
import com.peach.zk.constant.Constant;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zengtao on 2016/8/19.
 */
public class DistributedLock {

    private ZooKeeper zk;
    private static Logger logger = LoggerFactory.getLogger(DistributedLock.class);
    private String lockPath = null;
    private LockListener callBack;
    CountDownLatch countDownLatch = new CountDownLatch(1);

    public void init(ZooKeeper zk, LockListener lockListener) {
        this.zk = zk;
        this.callBack = lockListener;
    }

    public void getLock() {
        createLock();
        lock();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
    }

    public void unLock() {
        try {
            Stat stat = zk.exists(lockPath, false);
            if (stat != null) {
                zk.delete(lockPath, -1);
                callBack.lockReleased();
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
    }

    private synchronized void lock() {
        //logger.info("|| in lock");
        List<String> sortedChildren = getSortedChildren();
        try {
            int index = sortedChildren.indexOf(lockPath);
            switch (index) {
                case 0:
                    //logger.info("||so happy, I get my lock");
                callBack.lockAcquired();
                countDownLatch.countDown();
                break;
                default:
                    String preChildren = sortedChildren.get(index - 1);
                    Stat stat = zk.exists(preChildren, new NodeChangeWatcher());
                    if (stat == null) {
                        logger.warn("node {} has bean deleted", preChildren);
//                        Thread.sleep(3000);
                        lock();
                    }
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
        logger.info("|| out lock");
    }

    private void createLock() {
        try {
            Stat stat = zk.exists(Constant.ZK_LOCK_PATH, false);

            if (stat == null) {
                zk.create(Constant.ZK_LOCK_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            lockPath = zk.create(Constant.ZK_SUBLOCK_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException | InterruptedException e) {
            logger.error("|| create znode error", e);
        }
    }

    private List<String> getSortedChildren() {
        Ordering<String> order = Ordering.natural();
        List<String> fullpathChildrenList = Lists.newArrayList();
        List<String> sortedChildren = Lists.newArrayList();
        try {
            List<String> childrenList = zk.getChildren(Constant.ZK_LOCK_PATH, false);
            //需要得到children node 的全路径，后面可以匹配该node
            for (String children : childrenList) {
                fullpathChildrenList.add(Constant.ZK_LOCK_PATH + "/" + children);
            }
            sortedChildren = order.sortedCopy(fullpathChildrenList);
        } catch (KeeperException | InterruptedException e) {
            logger.error("||", e);
        }
        return sortedChildren;
    }

    private class NodeChangeWatcher implements Watcher {
        @Override
        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
                logger.info("|| pre node changed");
                lock();
            }
        }
    }


}
