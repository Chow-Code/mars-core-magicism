package org.alan.mars.listener;

import org.alan.mars.protostuff.PFSession;

/**
 * Created on 2017/10/25.
 *
 * @author Alan
 * @since 1.0
 */
public interface SessionCloseListener {
    void sessionClose(PFSession session);
}
