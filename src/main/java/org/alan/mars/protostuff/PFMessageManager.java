package org.alan.mars.protostuff;

import org.alan.mars.message.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * PF 消息管理器，将所有注解为@ProtobufMessage的class加载出来，并做好映射关系
 * <p>
 * <p>
 * Created on 2017/10/23.
 *
 * @author Alan
 * @since 1.0
 */
public class PFMessageManager {
    public static Map<Class<?>, ProtobufMessage> responseMap;

    public static Logger log = LoggerFactory.getLogger(PFMessageManager.class);

    public static PFMessage getPFMessage(Object msg) {
        return MessageUtil.getPfMessage(msg, responseMap);
    }

}
