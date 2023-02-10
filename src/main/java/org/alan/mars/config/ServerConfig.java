package org.alan.mars.config;

/**
 * Created on 2017/9/21.
 *
 * @author Alan
 * @since 1.0
 */
public class  ServerConfig {
    public int serverId;
    public String serverType;
    public String serverName;

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
