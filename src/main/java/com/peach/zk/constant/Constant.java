package com.peach.zk.constant;

/**
 * Created by zengtao on 2016/8/16.
 */
public class Constant {
    public static final String ZK_CONNECTION_STRING = "localhost:2181";
    public static final int ZK_SESSION_TIMEOUT = 50000;
    public static final String ZK_REGISTRY_PATH = "/registry";
    public static final String ZK_PROVIDER_PATH = ZK_REGISTRY_PATH + "/provider";
    public static final String ZK_LOCK_PATH = "/lock";
    public static final String ZK_SUBLOCK_PATH = ZK_LOCK_PATH + "/sub_lock";

}
