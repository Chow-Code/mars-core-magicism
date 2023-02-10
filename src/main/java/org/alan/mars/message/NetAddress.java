/**
 * Copyright Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 * <p>
 * 2017年2月27日
 */
package org.alan.mars.message;

import org.alan.mars.protostuff.ProtobufMessage;

import java.util.Objects;

/**
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(privately = true)
public class NetAddress {

    private String host;

    private int port;

    public NetAddress() {
    }

    public NetAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "[host=" + host + ", port=" + port + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetAddress that = (NetAddress) o;

        if (port != that.port) return false;
        return Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
