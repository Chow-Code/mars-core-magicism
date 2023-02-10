/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.net;

import org.alan.mars.message.NetAddress;

/**
 * <p>连接接口</p>
 * <p>
 * Created on 2017/3/27.
 *
 * @author Alan
 * @since 1.0
 */
public interface Connect {

    /**
     * 写消息方法
     *
     */
    boolean write(Object msg);

    /**
     * 关闭连接
     */
    void close();

    /**
     * 连接是否有效
     */
    boolean isActive();

    /**
     * 连接的地址
     */
    NetAddress address();

    /**
     * 当连接关闭
     */
    void onClose();

    /**
     * 当连接被创建
     */
    void onCreate();

    void addConnectListener(ConnectListener connectListener);

    void removeConnectListener(ConnectListener connectListener);
}
