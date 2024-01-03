package org.alan.mars.net;

/**
 * Created on 2017/3/28.
 *
 * @author Alan
 * @since 1.0
 */
public interface Inbox<T> {

    void onClusterReceive(Connect connect, T message);
}
