package org.alan.mars.cluster;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.executor.MarsWorkExecutor;
import org.alan.mars.listener.SessionReferenceBinder;
import org.alan.mars.message.PFMessage;
import org.alan.mars.message.PushCommandNotFound;
import org.alan.mars.message.PushInternalServerError;
import org.alan.mars.net.Connect;
import org.alan.mars.notify.INotifyRobot;
import org.alan.mars.notify.RobotNotifyContentBuilder;
import org.alan.mars.protostuff.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;

/**
 * Created on 2017/4/6.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class ClusterMessageDispatcher implements ApplicationListener<ContextRefreshedEvent> {

    private Map<Integer, MessageController> messageControllers;

    private final ClusterSystem clusterSystem;

    @Autowired(required = false)
    public SessionReferenceBinder sessionReferenceBinder;

    @Autowired(required = false)
    public MarsWorkExecutor marsWorkExecutor;
    @Autowired
    private INotifyRobot notifyRobot;
    @Autowired
    private RobotNotifyContentBuilder contentBuilder;

    public ClusterMessageDispatcher(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    public PFSession getPFSession(String id) {
        return clusterSystem.getSession(id);
    }

    public void onClusterReceive(Connect connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.sessionId;
        PFSession session = null;
        if (StrUtil.isNotBlank(sessionId)) {
            session = getPFSession(sessionId);
            if (session != null) {
                session.setActiveTime(System.currentTimeMillis());
            } else if (sessionReferenceBinder != null) {
                session = new PFSession(sessionId, connect, null);
                //session.setReference();
                session.setUserId(clusterMessage.userId);
                session.setPlayerId(clusterMessage.playerId);
                sessionReferenceBinder.bind(session, clusterMessage.userId, clusterMessage.playerId);
            }
        }
        PFMessage msg = clusterMessage.msg;
        try {
            final PFSession pfSession = session;
            // 此处如果用户有工作ID，采用工作线程提交
            if (session != null && session.getTaskGroup() != null) {
                // 绑定任务组
                session.getTaskGroup().submit(() -> handle(connect, pfSession, msg));
            } else if (session != null && session.getWorkId() > 0 && marsWorkExecutor != null) {
                // 此处如果用户有工作ID，采用工作线程提交
                marsWorkExecutor.submit(session.getWorkId(), () -> handle(connect, pfSession, msg));
            } else {
                handle(connect, session, msg);
            }
        } catch (Exception e) {
            log.warn("", e);
        }
    }

    public void handle(Connect connect, PFSession session, PFMessage msg) {
        int messageType = 0;
        int command = 0;
        int reqId = 0;
        Object bean;
        Class<?> logClazz = null;
        try {
            messageType = msg.messageType;
            command = msg.cmd;
            reqId = msg.reqId;
            MessageController messageController = messageControllers.get(messageType);

            if (messageController != null) {
                MethodInfo methodInfo = messageController.MethodInfos.get(command);
                if (methodInfo == null) {
                    log.warn("找不到处理函数,messageType = {},cmd = {}, reqId = {}", messageType, command, reqId);
                    if (session != null) {
                        session.send(new PushCommandNotFound(messageType, command), reqId);
                        notifyRobot.send(contentBuilder.commandNotFind(messageType, command, reqId, session.getPlayerId()), false);
                    }
                    return;
                }
                bean = messageController.been;
                if (methodInfo.params != null && methodInfo.params.length > 0) {
                    Object[] args = new Object[methodInfo.params.length];
                    Object reference = null;
                    if (session != null) {
                        reference = session.getReference();
                    }
                    for (int i = 0; i < args.length; i++) {
                        Class<?> clazz = methodInfo.params[i];
                        if (clazz == PFSession.class) {
                            args[i] = session;
                        } else if (reference != null && clazz == reference.getClass()) {
                            args[i] = reference;
                        } else if (Connect.class.isAssignableFrom(clazz)) {
                            args[i] = connect;
                        } else {
                            logClazz = clazz;
                            args[i] = ProtostuffUtil.deserialize(msg.data, clazz);
                        }
                    }
                    Object invoke = messageController.methodAccess.invoke(bean, methodInfo.index, args);
                    if (invoke != null && session != null) {
                        session.send(invoke, reqId);
                    }
                } else {
                    Object invoke = messageController.methodAccess.invoke(bean, methodInfo.index);
                    if (invoke != null) {
                        session.send(invoke, reqId);
                    }
                }
            } else {
                log.warn("未被注册的消息,messageType = {}, cmd = {}, reqId = {}", messageType, command, reqId);
                if (session != null) {
                    session.send(new PushCommandNotFound(messageType, command), reqId);
                    notifyRobot.send(contentBuilder.commandNotFind(messageType, command, reqId, session.getPlayerId()), false);
                }
            }
        } catch (Exception e) {
            if (session != null) {
                session.send(new PushInternalServerError(messageType, command), reqId);
                String json = null;
                if (logClazz != null && msg.data != null) {
                    try {
                        Object o = ProtostuffUtil.deserialize(msg.data, logClazz);
                        json = JSONUtil.toJsonStr(o);
                    } catch (Exception speError) {
                        json = "序列化失败, class = " + logClazz.getSimpleName();
                    }
                }
                log.error("消息内部错误[{}-{}-{}], playerId = {}, userId = {}, params = {}", messageType, command, reqId, session.getPlayerId(), session.getUserId(), json, e);
                notifyRobot.send(contentBuilder.commandException(messageType, command, reqId, session.getPlayerId(), json, e), false);
            } else {
                log.error("消息解析错误[{}-{}-{}]", messageType, command, reqId, e);
                notifyRobot.send(contentBuilder.commandException(messageType, command, reqId, 0, null, e), false);
            }
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        messageControllers = MessageUtil.load(event.getApplicationContext());
        MessageUtil.loadResponseMessage("org.alan", "com.xiaoxi");
        messageControllers.forEach((key, value) -> log.info("消息处理器[{}]->{}", key, value.been.getClass().getName()));
    }
}
