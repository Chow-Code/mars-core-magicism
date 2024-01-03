package org.alan.mars.cluster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.message.BroadCastMessage;
import org.alan.mars.message.PFMessage;
import org.alan.mars.protostuff.MessageUtil;
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
@RequiredArgsConstructor
public class ClusterMsgSender {

    private final ClusterSystem clusterSystem;

    /**
     * 向所有网关广播消息
     */
    private void broadcast2Gates(Object msg, Set<Long> playerIds, Set<Integer> serverAreas) {
        List<ClusterClient> clusterClients = clusterSystem.getAllGate();
        if (clusterClients != null && !clusterClients.isEmpty()) {
            clusterClients.forEach(clusterClient -> {
                try {
                    PFMessage message = MessageUtil.getPFMessage(msg);
                    BroadCastMessage broadCastMessage = new BroadCastMessage(message);
                    broadCastMessage.playerIds = playerIds;
                    broadCastMessage.serverAreas = serverAreas;
                    PFMessage pfMessage = MessageUtil.getPFMessage(broadCastMessage);
                    ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
                    clusterClient.write(clusterMessage);
                    log.debug("广播消息成功");
                } catch (Exception e) {
                    log.warn("广播消息到网关失败, gateName=" + clusterClient.nodeConfig.getName(), e);
                }
            });
        }
    }

    public void broadcast2Players(Object msg, Set<Long> playerIds) {
        broadcast2Gates(msg, playerIds, null);
    }

    public void broadcast2ServerAreas(Object msg, Set<Integer> serverAreas) {
        broadcast2Gates(msg, null, serverAreas);
    }

    public void broadcast2All(Object msg) {
        broadcast2Gates(msg, null, null);
    }
}
