package org.alan.mars.ws;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.alan.mars.gate.GateSession;

/**
 * Created on 2018/4/12.
 *
 * @author Alan
 * @since 1.0
 */
public class WSGateSession extends GateSession {
    @Override
    protected void checkServer() {
        AttributeKey<String> key = AttributeKey.valueOf("X-Real_IP");
        if (channel.hasAttr(key)) {
            Attribute<String> attribute = channel.attr(key);
            if (attribute != null && attribute.get() != null) {
                remoteAddress.setHost(attribute.get());
            }
        }
        super.checkServer();
    }
}
