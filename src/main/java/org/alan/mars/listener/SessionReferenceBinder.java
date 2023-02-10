package org.alan.mars.listener;

import org.alan.mars.protostuff.PFSession;

/**
 * Created on 2019/5/21.
 *
 * @author Alan
 * @since 1.0
 */
public interface SessionReferenceBinder {

    void bind(PFSession session, long userId);
}
