package org.alan.mars.tips;


import org.alan.mars.protostuff.ProtobufMessage;

/**
 * 游戏通用返回值枚举定义
 * <p>
 * Created on 2017/3/27.
 *
 * @author Chow
 * @since 1.0
 */
@ProtobufMessage(desc = "服务器状态")
public enum MarsResultEnum {
    NETWORK_CANT_USE(0, "网络不可用"),
    NETWORK_SUCCESS(1, "连接成功"),
    SERVICE_CANT_USE(2, "服务不可用"),
    PLAYER_KICK_OUT(3, "被别的设备挤下去"),
    OPERATIONS_KICK_OUT(4,"运营主动踢"),
    ;

    public final int code;
    public final String message;

    MarsResultEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
