/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.net;

import lombok.Getter;
import lombok.Setter;
import org.alan.mars.message.NetAddress;

/**
 * Session 类抽象接口
 * <p>
 * 泛型类型 T 表示该session可发送的消息类型
 * <p>
 * 泛型类型 M 表示该session中连接可发送的消息类型
 * <p>
 * Created on 2017/3/30.
 *
 * @author Alan
 * @since 1.0
 */
@Getter
@Setter
public abstract class Session implements ConnectListener {
    protected String sessionId;
    protected Connect connect;
    protected NetAddress address;
    protected Object reference;
    protected SessionListener sessionListener;

    public Session(String sessionId, Connect connect, NetAddress address) {
        this.sessionId = sessionId;
        this.connect = connect;
        this.address = address;
    }

    public abstract void send(Object msg);

    public void close() {
        if (connect != null && connect.isActive()) {
            connect.close();
        }
    }

    @Override
    public void onConnectClose(Connect connect) {
        if (sessionListener != null) {
            sessionListener.onSessionClose(this);
        }
    }

    @Override
    public String toString() {
        return "Session{sessionId=" + sessionId + ", connect=" + connect + ", address=" + address + ",hash" + hashCode() + '}';
    }
}
