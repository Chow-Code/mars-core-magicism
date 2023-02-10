/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.curator;

import com.alibaba.fastjson.JSON;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.cluster.ClusterHelper;

import java.util.*;

/**
 * <p>节点</p>
 * <p>
 * Created by Alan (mingweiyang@foxmail.com) on 2017/3/7.
 *
 * @since 1.0
 */
public class MarsNode {
    /**
     * 节点路径
     */
    private String nodePath;
    /**
     * 节点数据
     */
    private String nodeData;
    /* 节点信息*/
    private NodeConfig nodeConfig;
    /**
     * 子节点
     */
    private final Map<String, MarsNode> childrenNodes = new HashMap<>();

    private String strStat;

    public String getStrStat() {
        return strStat;
    }

    public void setStrStat(String strStat) {
        this.strStat = strStat;
    }

    public MarsNode(String nodePath, String nodeData , String strStat) {
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

        List<MarsNode> childrens = getAllChildren();
        List<MarsNode> tempNodes = new ArrayList<>();
        List<MarsNode> preciselist = new ArrayList<>();
        for (MarsNode marsNode : childrens) {
            NodeConfig nodeConfig = marsNode.getNodeConfig();
            if (nodeConfig == null){
                continue;
            }
            if ((nodeConfig.whiteIdList == null || nodeConfig.whiteIdList.length == 0)
                    && (nodeConfig.whiteIpList == null || nodeConfig.whiteIpList.length == 0)) {
                tempNodes.add(marsNode);
            } else if (ClusterHelper.preciseInIdWhiteList(id, nodeConfig.whiteIdList) || ClusterHelper.preciseInIpWhiteList(ip, nodeConfig.whiteIpList)) {
                preciselist.add(marsNode);
            }
        }
        if (!preciselist.isEmpty()) {
            tempNodes = preciselist;
        }
        if (tempNodes.isEmpty()) {
            return null;
        }

        int totalWeight = tempNodes.stream().mapToInt(m -> m.getNodeConfig().weight).sum();

        if (totalWeight <= 0) {
            return null;
        }

        Random random = new Random();
        int p = random.nextInt(totalWeight);

        int sum = 0;
        for (MarsNode marsNode : tempNodes) {
            sum += marsNode.getNodeConfig().weight;
            if (p < sum) {
                return marsNode;
            }
        }
        System.out.println("===========================");
        System.out.println("== 根据权重寻找节点失败");
        System.out.println("===========================");
        return null;
    }

    public boolean hasChildren() {
        return !childrenNodes.isEmpty();
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getNodeData() {
        return nodeData;
    }

    public NodeConfig getNodeConfig() {
        if (nodeConfig == null && nodeData != null && !nodeData.isEmpty()) {
            try {
                nodeConfig = JSON.parseObject(nodeData, NodeConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
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
