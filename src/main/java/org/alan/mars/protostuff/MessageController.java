package org.alan.mars.protostuff;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.util.Map;

/**
 * Created on 2017/7/28.
 *
 * @author Alan
 * @since 1.0
 */
public class MessageController {
    public final Object been;
    public final MethodAccess methodAccess;
    public final Map<Integer, MethodInfo> MethodInfos;

    public MessageController(Object been) {
        this.been = been;
        Class<?> clazz = been.getClass();
        methodAccess = MethodAccess.get(clazz);
        MethodInfos = MessageUtil.load(methodAccess, clazz);
    }

    public MessageController(Object been, Class<?> clazz) {
        this.been = been;
        //Class<?> clazz = been.getClass();
        methodAccess = MethodAccess.get(clazz);
        MethodInfos = MessageUtil.load(methodAccess, clazz);
    }
}
