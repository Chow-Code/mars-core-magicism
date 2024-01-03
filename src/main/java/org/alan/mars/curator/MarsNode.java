package org.alan.mars.curator;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.utils.RandomUtil;

import java.util.*;

/**
 * 节点
 * Created by Alan (mingweiyang@foxmail.com) on 2017/3/7.
 *
 * @since 1.0
 */
@Getter
@Setter
@Slf4j
@ToString
public class MarsNode {
    /**
     * 节点路径
     */
    @Setter
    private String nodePath;
    /**
     * 节点数据
     */
    private String nodeData;
    /**
     * 节点信息
     */
    private NodeConfig nodeConfig;
    /**
     * 子节点
     */
    private final Map<String, MarsNode> childrenNodes = new HashMap<>();

    @Setter
    private String strStat;

    public MarsNode(String nodePath, String nodeData, String strStat) {
        this.nodePath = nodePath;
        this.nodeData = nodeData;
        this.strStat = strStat;
    }

    public MarsNode addChildren(MarsNode marsNode) {
        MarsNode mn = getChildren(marsNode.getNodePath(), false);
        // 找不到或者是本层目录的节点，则直接更新
        if (mn == null || mn.getNodePath().equals(marsNode.getNodePath())) {
            return childrenNodes.put(marsNode.getNodePath(), marsNode);
        } else {
            return mn.addChildren(marsNode);
        }
    }

    public MarsNode removeChildren(String path) {
        if (childrenNodes.containsKey(path)) {
            return childrenNodes.remove(path);
        } else {
            MarsNode marsNode = getChildren(path, false);
            if (marsNode != null) {
                return marsNode.removeChildren(path);
            }
        }
        return null;
    }

    public MarsNode getChildren(String path, boolean rc) {
        if (!childrenNodes.isEmpty()) {
            for (MarsNode marsNode : childrenNodes.values()) {
                if (path.equals(marsNode.getNodePath())) {
                    return marsNode;
                }
                if (path.startsWith(marsNode.getNodePath())) {
                    if (rc) {
                        return marsNode.getChildren(path, true);
                    }
                    return marsNode;
                }
            }
        }
        return null;
    }

    /**
     * 获取本节点下的所有子节点
     */
    public List<MarsNode> getAllChildren() {
        return new ArrayList<>(childrenNodes.values());
    }

    /**
     * 从节点中随机选择一个子节点，如果节点不包含子节点返回null
     */
    public MarsNode randomOneMarsNode() {
        if (childrenNodes.isEmpty()) {
            return null;
        }
        List<MarsNode> children = getAllChildren();
        Random random = new Random();
        int p = random.nextInt(children.size());
        return children.get(p);
    }

    public MarsNode randomOneMarsNodeWithWeight(String ip, long id) {
        if (childrenNodes.isEmpty()) {
            return null;
        }

        List<MarsNode> tempNodes = canInNodes(getAllChildren(), ip, id);
        if (tempNodes == null){
            return null;
        }
        int index = RandomUtil.randomWidget(tempNodes.stream().mapToInt(m -> m.getNodeConfig().weight).toArray());
        if (index < 0) {
            log.error("根据权重寻找节点失败, nodes = {}", tempNodes);
            return null;
        }
        return tempNodes.get(index);
    }

    /**
     * 判断是否在白名单中
     *
     * @param nodeConfig 节点配置
     * @param ip         玩家IP
     * @param playerId   玩家ID
     * @return 在白名单返回 true 白名单为空返回false
     */
    public static boolean inWhiteList(NodeConfig nodeConfig, String ip, long playerId) {
        if (whitelistClosed(nodeConfig)) {
            return false;
        }
        return ArrayUtil.contains(nodeConfig.whiteIdList, playerId) || ArrayUtil.contains(nodeConfig.whiteIpList, ip);
    }

    /**
     * 判断玩家是否可以进入某个节点
     *
     * @param nodeConfig 节点配置
     * @param ip         玩家IP
     * @param playerId   玩家ID
     * @return 可以进入返回true
     */
    public static boolean canIn(NodeConfig nodeConfig, String ip, long playerId) {
        if (whitelistClosed(nodeConfig)) {
            return true;
        }
        return inWhiteList(nodeConfig, ip, playerId);
    }

    /**
     * 判断白名单是否已关闭
     *
     * @param nodeConfig 节点配置
     * @return 已关闭返回 true
     */
    public static boolean whitelistClosed(NodeConfig nodeConfig) {
        return ArrayUtil.isEmpty(nodeConfig.whiteIdList) && ArrayUtil.isEmpty((nodeConfig.whiteIpList));
    }

    public static List<MarsNode> canInNodes(List<MarsNode> wait, String address, long id) {
        List<MarsNode> tempNodes = new ArrayList<>();
        List<MarsNode> preciselist = new ArrayList<>();
        for (MarsNode marsNode : wait) {
            NodeConfig nodeConfig = marsNode.getNodeConfig();
            if (nodeConfig == null) {
                continue;
            }
            if (address == null && id == 0) {
                preciselist.add(marsNode);
            }
            if (whitelistClosed(nodeConfig)) {
                tempNodes.add(marsNode);
            } else if (inWhiteList(nodeConfig, address, id)) {
                preciselist.add(marsNode);
            }
        }
        if (!preciselist.isEmpty()) {
            tempNodes = preciselist;
        }
        if (tempNodes.isEmpty()) {
            return null;
        }
        return tempNodes;
    }

    public boolean hasChildren() {
        return !childrenNodes.isEmpty();
    }

    public NodeConfig getNodeConfig() {

        if (nodeConfig == null && JSONUtil.isTypeJSON(nodeData)) {
            try {
                nodeConfig = JSONUtil.toBean(nodeData, NodeConfig.class);
            } catch (Exception e) {
                log.error("无法转换nodeData:{}", nodeData, e);
            }
        }
        return nodeConfig;
    }

    public void updateData(String data) {
        this.nodeData = data;
        nodeConfig = null;
        getNodeConfig();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarsNode)) return false;

        MarsNode marsNode = (MarsNode) o;

        return nodePath.equals(marsNode.nodePath);
    }

    @Override
    public int hashCode() {
        return nodePath.hashCode();
    }
}
