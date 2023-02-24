/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.cluster;

import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.executor.MarsWorkExecutor;
import org.alan.mars.message.PFMessage;
import org.alan.mars.protostuff.*;
import org.alan.mars.listener.SessionReferenceBinder;
import org.alan.mars.net.Connect;
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
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@ChannelHandler.Sharable
public class ClusterMessageDispatcher implements ApplicationListener<ContextRefreshedEvent> {

    private Map<Integer, MessageController> messageControllers;

    ClusterSystem clusterSystem;

    @Autowired(required = false)
    public SessionReferenceBinder sessionReferenceBinder;

    @Autowired(required = false)
    public MarsWorkExecutor marsWorkExecutor;

    public ClusterMessageDispatcher(ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
    }

    public PFSession getPFSession(String id) {
        return clusterSystem.sessionMap().get(id);
    }

    public void onClusterReceive(Connect connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.sessionId;
        PFSession session = null;
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            session = getPFSession(sessionId);
            if (session != null) {
                session.activeTime = System.currentTimeMillis();
            } else if (sessionReferenceBinder != null) {
                session = new PFSession(sessionId, connect, null);
                //session.setReference();
                session.userId = clusterMessage.userId;
                sessionReferenceBinder.bind(session, clusterMessage.userId);
            }
        }
        PFMessage msg = clusterMessage.msg;
        try {
            final PFSession pfSession = session;
            // 此处如果用户有工作ID，采用工作线程提交
            if (session != null && session.workId > 0 && marsWorkExecutor != null) {
                marsWorkExecutor.submit(session.workId, () -> handle(connect, pfSession, msg));
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
        try {
            messageType = msg.messageType;
            command = msg.cmd;
            MessageController messageController = messageControllers.get(messageType);
            if (messageController != null) {
                MethodInfo methodInfo = messageController.MethodInfos.get(command);
                if (methodInfo == null) {
                    log.warn("找不到处理函数,messageType={},cmd={}", messageType, command);
                    return;
                }
                Object bean = messageController.been;
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
                            if (msg.data != null && msg.data.length > 0) {
                                args[i] = ProtostuffUtil.deserialize(msg.data, clazz);
                            }
                        }
                    }
                    Object invoke = messageController.methodAccess.invoke(bean, methodInfo.index, args);
                    if (invoke != null && session != null){
                        session.send(invoke);
                    }
                } else {
                    Object invoke = messageController.methodAccess.invoke(bean, methodInfo.index);
                    if (invoke != null){
                        session.send(invoke);
                    }
                }
            } else {
                log.warn("未被注册的消息,messageType={},cmd={}", messageType, command);
            }
        } catch (Exception e) {
            log.warn("消息解析错误,messageType=" + messageType + ",cmd=" + command, e);
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        messageControllers = MessageUtil.load(event.getApplicationContext());
        MessageUtil.loadResponseMessage("org.alan", "com.xiaoxi");
        messageControllers.forEach((key, value) -> log.info("消息处理器[{}]->{}", key, value.been.getClass().getName()));
    }
}
