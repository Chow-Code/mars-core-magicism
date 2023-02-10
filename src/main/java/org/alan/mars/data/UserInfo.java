/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.data;

import org.alan.mars.message.NetAddress;

/**
 * 用户信息
 * <p>
 * Created on 2017/4/13.
 *
 * @author Alan
 * @since 1.0
 */
public class UserInfo {
    public static final String USER_INFO = "USER_INFO:";
    /* 第三方平台ID，在接入第三方时有用*/
    private String pfUserId;
    /*平台用户名*/
    private String pfUserName;
    /* 用户ID*/
    public long userId;
    /* 用户名称*/
    public String userName;
    /* 用户当前的认证信息*/
    public String token;
    /* 用户当前是否在线，避免重复登录*/
    public boolean online;
    /* 用户当前登录的大区*/
    public int currentZone;
    /* 用户当前所在机器的地址*/
    public NetAddress logicAddress;
}
