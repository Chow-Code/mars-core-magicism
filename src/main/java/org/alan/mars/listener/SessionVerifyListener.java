package org.alan.mars.listener;

/**
 * Created on 2017/10/24.
 *
 * @author Alan
 * @since 1.0
 */
public interface SessionVerifyListener {
    void userVerifyPass(String sessionId, long userId,String ip);
}
