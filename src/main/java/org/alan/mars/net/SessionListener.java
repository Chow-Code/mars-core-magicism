/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.net;

/**
 * 回话监听器
 * <p>
 * Created on 2017/4/10.
 *
 * @author Alan
 * @since 1.0
 */
public interface SessionListener {

    /**
     * 当回话被关闭
     */
    void onSessionClose(Session session);

}
