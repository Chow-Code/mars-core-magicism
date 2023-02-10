/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.cluster;

import org.alan.mars.message.NetAddress;

/**
 * <p>
 * 集群服务
 * <p>
 * Created on 2017/3/31.
 *
 * @author Alan
 * @since 1.0
 */
public interface ClusterService {

    /**
     * 用户进入
     */
    boolean sessionEnter(String sessionId, NetAddress netAddress);

    /**
     * 用户退出
     */
    boolean sessionQuit(String sessionId);

    /**
     * 用户消息到达
     */
    void messageReceive(String sessionId, Object msg);
}
