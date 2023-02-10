package org.alan.mars.cluster;

import java.util.Arrays;

/**
 * Created on 2018/1/3.
 *
 * @author Alan
 * @since 1.0
 */
public class ClusterHelper {
    public static boolean inIpWhiteList(String ip, String[] whiteList) {
        if (whiteList == null || whiteList.length == 0) {
            return true;
        }
        return Arrays.asList(whiteList).contains(ip);
    }

    public static boolean preciseInIpWhiteList(String ip, String[] whiteList) {
        if (whiteList == null || whiteList.length == 0) {
            return false;
        }
        return Arrays.asList(whiteList).contains(ip);
    }

    public static boolean inIdWhiteList(long id, int[] whiteList) {
        if (whiteList == null || whiteList.length == 0) {
            return true;
        }
        for (long wid : whiteList) {
            if (wid == id) {
                return true;
            }
        }
        return false;
    }

    public static boolean preciseInIdWhiteList(long id, int[] whiteList) {
        if (whiteList == null) {
            return false;
        }
        for (long wid : whiteList) {
            if (wid == id) {
                return true;
            }
        }
        return false;
    }
}
