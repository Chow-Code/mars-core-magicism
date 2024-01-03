package org.alan.mars.net;

/**
 * 连接监听器
 * <p>
 * Created on 2017/3/29.
 *
 * @author Alan
 * @since 1.0
 */
public interface ConnectListener {

    /**
     * 当连接被关闭
     */
    void onConnectClose(Connect connect);
}
