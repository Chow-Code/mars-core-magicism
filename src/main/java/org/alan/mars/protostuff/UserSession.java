package org.alan.mars.protostuff;

import lombok.Getter;
import lombok.Setter;
import org.alan.mars.net.Connect;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2017/8/2.
 *
 * @author Alan
 * @since 1.0
 */
@Getter
@Setter
public class UserSession {
    /* 属性表*/
    public Map<String, Object> attributeMap = new HashMap<>();
    /* 用户连接*/
    private Connect connect;
    /* 会话ID*/
    private String sessionId;

    public void writeMessage(Object t) {
        connect.write(t);
    }

    public UserSession(Connect connect, String sessionId) {
        this.connect = connect;
        this.sessionId = sessionId;
    }
}
