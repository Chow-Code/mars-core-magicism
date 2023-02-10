/**
 * Copyright Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 * <p>
 * 2017年2月8日
 */
package org.alan.mars.net;

import org.alan.mars.message.NetAddress;
import org.alan.mars.message.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户session
 *
 * @author Alan
 */
public class MarsSession extends Session implements Inbox<PFMessage> {

    protected Logger log = LoggerFactory.getLogger(getClass());


    public MarsSession(String id, NetAddress netAddress, Connect connect) {
        super(id, connect, netAddress);
    }

    @Override
    public void send(Object g) {
        
    }

    @Override
    public void onClusterReceive(Connect connect, PFMessage message) {
    }
}
