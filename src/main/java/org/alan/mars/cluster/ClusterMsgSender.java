package org.alan.mars.cluster;

import lombok.extern.slf4j.Slf4j;
import org.alan.mars.protostuff.MessageUtil;
import org.alan.mars.message.PFMessage;
import org.alan.mars.message.BroadCastMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 集群消息发送器
 * <p>
 * Created on 2020/3/19.
 *
 * @author Alan
 * @since 1.0
 */
@Component
@Slf4j
public class ClusterMsgSender {

    @Autowired
    private ClusterSystem clusterSystem;
    /**
     * 向所有网关广播消息
     */
    public void broadcast2Gates(Object msg , Set<Long> playerIds) {
        List<ClusterClient> clusterClients = clusterSystem.getAllGate();
        if (clusterClients != null && !clusterClients.isEmpty()) {
            clusterClients.forEach(clusterClient -> {
                try {
                    PFMessage message = MessageUtil.getPFMessage(msg);
                    BroadCastMessage msg1 = new BroadCastMessage(message);
                    msg1.playerIds = playerIds;
                    PFMessage pfMessage = MessageUtil.getPFMessage(msg1);
                    ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
                    clusterClient.write(clusterMessage);
                    log.debug("广播消息成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("广播消息到网关失败,gateName=" + clusterClient.nodeConfig.getName(), e);
                }
            });
        }
    }
    public void broadcast2Gates(Object msg) {
        broadcast2Gates(msg,null);
    }
}
