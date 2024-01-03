package org.alan.mars.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Created on 2017/9/21.
 *
 * @author Alan
 * @since 1.0
 */
@Getter
@Setter
public class ServerConfig {
    public int serverId;
    public String serverType;
    public String serverName;
}
