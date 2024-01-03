package org.alan.mars.message;

import lombok.Getter;
import lombok.Setter;
import org.alan.mars.protostuff.ProtobufMessage;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(privately = true)
@Getter
@Setter
public class NetAddress {

    private String host;

    private Integer port;

    public NetAddress() {
    }

    public NetAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }
    @Override
    public String toString() {
        return "[" + host + ":" + port + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof NetAddress)) return false;

        NetAddress that = (NetAddress) o;

        return new EqualsBuilder().append(host, that.host).append(port, that.port).isEquals();
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
