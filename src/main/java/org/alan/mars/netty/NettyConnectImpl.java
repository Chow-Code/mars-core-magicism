/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.netty;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2017/4/5.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class NettyConnectImpl extends NettyConnect {

    @Override
    public void onClose() {
        log.debug("connect on closed.");
    }

    @Override
    public void onCreate() {
        log.debug("connect on create");
    }

    @Override
    public void messageReceived(Object msg) {
        log.debug("message received...");
    }
}
