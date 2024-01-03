package org.alan.mars.listener;

import org.alan.mars.message.SessionVerifyPass;

/**
 * Created on 2017/10/24.
 *
 * @author Alan
 * @since 1.0
 */
public interface SessionVerifyListener {
    void userVerifyPass(SessionVerifyPass sessionVerifyPass);
}
