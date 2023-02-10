/**
 * Copyright Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 *
 * 2017年3月2日 	
 */
package org.alan.mars.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * zk 配置信息
 * 
 * @scene 1.0
 * 
 * @author Alan
 *
 */
@Configuration
@ConfigurationProperties(prefix = "zookeeper")
@Getter
@Setter
public class ZookeeperConfig {
	/**
	 * 连接字符串
	 */
	String connects;
	/**
	 * 连接间隔时间
	 */
	int baseSleepTimeMs;
	/**
	 * 最大重试次数
	 */
	int maxRetries;
	/**
	 * 根目录
	 */
	String marsRoot;
	/**
	 * zk session 超时时间
	 */
	int sessionTimeoutMs = 30000;
	/**
	 * zk 连接 超时时间
	 */
	int connectionTimeoutMs = 3000;

}
