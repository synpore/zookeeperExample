package com.peach.zk.zkrmi;

import com.peach.zk.constant.Constant;
import com.peach.zk.util.ZkUtil;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by zengtao on 2016/8/16.
 */
public class ServiceConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConsumer.class);
    //用于等待SyncConnected事件触发后继续执行当前线程
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    //用于保存最近的rmi地址(考虑到该变量或许会被其它线程所修改，一旦修改后，该变量的值会影响到所有线程）
    private volatile List<String> urlList = new ArrayList<String>();

    public ServiceConsumer() throws InterruptedException {
        ZooKeeper zk = ZkUtil.connectServer(countDownLatch);
        if (zk != null) {
            watchNode(zk);
            zk.close();
        }
    }

    //查找RMI服务
    public <T extends Remote> T lookup() {
        T service = null;
        int size = urlList.size();
        if (size > 0) {
            String url;
            if (size == 1) {
                url = urlList.get(0);
                logger.info("only url : {}", url);
            } else {
                url = urlList.get(ThreadLocalRandom.current().nextInt(size));
                logger.info("using random url : {}", url);
            }
            service = lookupService(url);
        }
        return service;
    }

    private <T extends Remote> T lookupService(String url) {
        T remote = null;
        try {
            remote = (T) Naming.lookup(url);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            logger.error("", e);
            //若连接中断，则使用urlList中第一个RMI地址来查找
            //简单的重试方式，确保不会抛出异常
            if(e instanceof ConnectException){
                logger.error("ConnectException -> url: {}", url);
                if(urlList.size() != 0){
                    url = urlList.get(0);
                    return lookupService(url);
                }
            }
            logger.error("", e);
        }
        return remote;
    }

    private void watchNode(final ZooKeeper zk) {

        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                        //如果子节点有变化，则重新调用该方法（为了获取最新的子节点中的数据）
                        watchNode(zk);
                    }
                }
            });
            List<String> dataList = new ArrayList<String>();
            for (String node : nodeList) {
                byte[] data = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(data));
            }
            logger.info("node data: {}", dataList);
            urlList = dataList;
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
    }
}