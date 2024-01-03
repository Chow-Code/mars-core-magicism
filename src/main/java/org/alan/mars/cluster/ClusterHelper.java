package org.alan.mars.cluster;

import cn.hutool.core.util.ArrayUtil;

/**
 * Created on 2018/1/3.
 *
 * @author Alan
 * @since 1.0
 */
public class ClusterHelper {
    /**
     * 判断ip地址是否在白名单
     *
     * @param ip        要判断的IP地址
     * @param whiteList 当前的白名单列表
     * @return true 带表在白名单中 如果白名单为空则返回true
     */
    public static boolean inIpWhiteList(String ip, String[] whiteList) {
        if (ArrayUtil.isEmpty(whiteList)) {
            return true;
        }
        return ArrayUtil.contains(whiteList, ip);
    }

    /**
     * 精确判断ip地址是否在白名单
     *
     * @param ip        要判断的IP地址
     * @param whiteList 当前的白名单列表
     * @return true 带表在白名单中
     */
    public static boolean preciseInIpWhiteList(String ip, String[] whiteList) {
        return ArrayUtil.contains(whiteList, ip);
    }

    /**
     * 判断id是否在白名单
     *
     * @param id        要判断的ID
     * @param whiteList 当前的白名单列表
     * @return true 带表在白名单中 如果白名单为空则返回true
     */
    public static boolean inIdWhiteList(long id, long[] whiteList) {
        if (ArrayUtil.isEmpty(whiteList)) {
            return true;
        }
        return ArrayUtil.contains(whiteList, id);
    }

    public static boolean preciseInIdWhiteList(long id, long[] whiteList) {
        return ArrayUtil.contains(whiteList, id);
    }
}
