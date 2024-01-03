/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alan.mars.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数工具
 */
public class RandomUtil {

    /**
     * 随机一个数
     *
     * @param max 最大数字
     * @return 0 - 最大数(不包含) 中的一个
     */
    public static int random(int max) {
        if (max == 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * 包含最大最小值
     */
    public static int random(int min, int max) {
        if (max - min <= 0) {
            int temp = min;
            min = max;
            max = temp;
        }
        return min + random(max - min + 1);
    }

    /**
     * 通过区间来计算一个确定值
     *
     * @param interval 该数组的长度应该为2
     * @return 值
     */
    public static int random(int[] interval) {
        if (interval.length < 2) {
            return interval[0];
        }
        return random(interval[0], interval[1]);
    }

    /**
     * 通过权重来随机获取下标
     *
     * @param weights 每个对象的权重值
     * @return 随机下标
     */
    public static int randomWidget(int[] weights) {
        if (weights.length == 0){
            return -1;
        }
        if (weights.length == 1) {
            return 0;
        }
        int max = Arrays.stream(weights).sum();
        if (max < 0){
            return -1;
        }
        int random = random(1,max);
        int current = 0;
        for (int i = 0; i < weights.length; i++) {
            if ( weights[i] <= 0){
                continue;
            }
            current += weights[i];
            if (current >= random) {
                return i;
            }
        }
        return 0;
    }

    public static int randomWidget(List<Integer> weights) {
        if (weights.size() == 1) {
            return 0;
        }
        int max = weights.stream().mapToInt(i -> i).sum();
        int random = random(1,max);
        int current = 0;
        for (int i = 0; i < weights.size(); i++) {
            int count = weights.get(i);
            if (count <= 0){
                continue;
            }
            current += count;
            if (current >= random) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 通过权重获数字
     *
     * @param weights 2维数组 每个元素为长度为2的数组 下标为0则代表将要随机的数字 下标为1则代表该随机数的权重
     * @return 数字
     */
    public static int randomWidget(int[][] weights) {
        return randomWidgetAndIndex(weights)[1];
    }

    /**
     * 通过权重获数字
     *
     * @param weights 2维数组 每个元素为长度为2的数组 下标0代表将要随机的数字 下标1代表该随机数的权重
     * @return int数组 0为数组下标 1为数字
     */
    public static int[] randomWidgetAndIndex(int[][] weights) {
        int[] res = new int[2];
        if (weights.length == 1) {
            res[1] = weights[0][0];
            return res;
        }
        int max = Arrays.stream(weights).mapToInt(item -> item[1]).sum();
        int random = random(1,max);
        int current = 0;

        for (int i = 0, weightsLength = weights.length; i < weightsLength; i++) {
            int[] weight = weights[i];
            if (weight[1] <= 0){
                continue;
            }
            current += weight[1];
            if (current >= random) {
                res[0] = i;
                res[1] = weight[0];
                break;
            }
        }
        return res;
    }

    /**
     * 通过权重获多个数字
     *
     * @param count   获取最多多少个数字
     * @param weights 2维数组 每个元素为长度为2的数组 下标0代表将要随机的数字 下标1代表该随机数的权重
     * @return 数字
     */
    public static int[] randomWidget(int[][] weights, int count) {
        if (weights.length <= count) {
            return Arrays.stream(weights).mapToInt(item -> item[0]).toArray();
        }
        int[][] newWeights = Arrays.copyOf(weights, weights.length);
        int[] res = new int[count];
        for (int i = 0; i < count; i++) {
            int[] indexes = Arrays.stream(newWeights).mapToInt(item -> item[1]).toArray();
            int index = randomWidget(indexes);
            res[i] = newWeights[index][0];
            //删除该元素
            newWeights = ArrayUtils.remove(newWeights, index);
        }
        return res;
    }


    /**
     * 通过概率随机出是否出现
     *
     * @param probability 概率 取值为 0~1 小于0必然会返回false 大于1必然会返回true
     * @return true代表会出现
     */
    public static boolean randomProbability(float probability) {
        if (probability <= 0){
            return false;
        }
        if (probability >= 1){
            return true;
        }
        int base = 10000;
        int integer = new Float(probability * base).intValue();
        int random = random(base);
        return integer >= random;
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 随机一个长度的字符串
     * 该字符串由 a-z A-Z 0-9 构成
     *
     * @param length 长度
     */
    public static String randomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
