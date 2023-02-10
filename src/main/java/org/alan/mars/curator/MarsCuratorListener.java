/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.curator;

/**
 * Created on 2017/3/9.
 *
 * @author Alan
 * @since 1.0
 */
public interface MarsCuratorListener {
    /**
     * 初始化完成
     */
    void marsCuratorRefreshed(MarsCurator marsCurator);
}
