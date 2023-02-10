/**
 * Copyright Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 *
 * 2017年2月27日 	
 */
package org.alan.mars.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * 
 *
 * @author Alan
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class ServerInfo {

    protected String host;

    protected int port;

    public ServerInfo(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

}
