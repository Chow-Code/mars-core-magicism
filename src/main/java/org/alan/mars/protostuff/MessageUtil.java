package org.alan.mars.protostuff;

import com.esotericsoftware.reflectasm.MethodAccess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.message.PFMessage;
import org.alan.mars.utils.ClassUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created on 2017/7/28.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class MessageUtil {

    public static Map<Class<?>, ProtobufMessage> responseMap;

    public static PFMessage getPFMessage(Object msg) {
        return getPfMessage(msg, responseMap);
    }

    public static ByteBuf encode(PFMessage msg){
        int len = 0;
        if (msg.data != null) {
            len = msg.data.length;
        }
        ByteBuf byteBuf = Unpooled.buffer(len + 4);
        byteBuf.writeShort(msg.messageType);
        byteBuf.writeShort(msg.cmd);
        byteBuf.writeBytes(msg.data);
        return byteBuf;
    }

    static PFMessage getPfMessage(Object msg, Map<Class<?>, ProtobufMessage> responseMap) {
        ProtobufMessage responseMessage = responseMap.get(msg.getClass());
        if (responseMessage == null) {
            log.warn("消息发送失败，该消息结构应该是有ProtobufMessage注解, 且resp = true，msg-class={}", msg.getClass());
            return null;
        }
        byte[] data = ProtostuffUtil.serialize(msg);
        return new PFMessage(responseMessage.messageType(), responseMessage.cmd(), data);
    }

    public static Map<Integer, MessageController> load(ApplicationContext context) {
        Map<Integer, MessageController> messageControllers = new HashMap<>();
        Class<MessageType> clazz = MessageType.class;
        log.debug("开始初始化 {} 消息分发器", clazz);
        Map<String, Object> beans = context.getBeansWithAnnotation(clazz);
        beans.values().forEach(o -> {
            MessageType messageType;
            if (AopUtils.isAopProxy(o)) {
                Class<?> targetClass = AopUtils.getTargetClass(o);
                messageType = targetClass.getAnnotation(MessageType.class);
                MessageController messageController = new MessageController(o, targetClass);
                messageControllers.put(messageType.value(), messageController);
            } else {
                messageType = o.getClass().getAnnotation(MessageType.class);
                MessageController messageController = new MessageController(o);
                messageControllers.put(messageType.value(), messageController);
            }

        });
        return messageControllers;
    }

    public static Map<Integer, MethodInfo> load(MethodAccess methodAccess, Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<Integer, MethodInfo> MethodInfos = new HashMap<>();
        for (Method method : methods) {
            Class<Command> clz = Command.class;
            Command command = method.getAnnotation(clz);
            if (command != null) {
                String name = method.getName();
                Class<?>[] types = method.getParameterTypes();
                Type returnType = method.getReturnType();
                int index = methodAccess.getIndex(name, types);
                MethodInfo methodInfo = new MethodInfo(index, name, types, returnType);
                MethodInfos.put(command.value(), methodInfo);
            }

        }
        return MethodInfos;
    }

    public static void loadResponseMessage(String... packages) {
        responseMap = new HashMap<>();
        Set<Class<?>> classes = new HashSet<>();
        for (String pkg : packages) {
            classes.addAll(ClassUtils.getAllClassByAnnotation(pkg, ProtobufMessage.class));
        }
        if (!classes.isEmpty()) {
            classes.forEach(clazz -> {
                ProtobufMessage responseMessage = clazz.getAnnotation(ProtobufMessage.class);
                if (responseMessage.resp()) {
                    responseMap.put(clazz, responseMessage);
                }
            });
        }
    }
}
