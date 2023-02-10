/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.curator;

/**
 * 节点监听器
 * <p>
 * Created on 2017/4/13.
 *
 * @author Alan
 * @since 1.0
 */
public interface MarsNodeListener {
    enum NodeChangeType {
        NODE_ADD, NODE_REMOVE, DATA_CHANGE
    }

    /**
     * 节点状态改变
     */
    void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode);
}
