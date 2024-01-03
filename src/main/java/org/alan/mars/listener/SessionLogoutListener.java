package org.alan.mars.listener;

/**
 * Created on 2017/11/23.
 *
 * @author Alan
 * @since 1.0
 */
public interface SessionLogoutListener {
    void logout( String sessionId, long userId, long playerId);
}
