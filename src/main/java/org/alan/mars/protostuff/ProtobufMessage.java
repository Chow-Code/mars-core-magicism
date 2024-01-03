package org.alan.mars.protostuff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记类型为Protobuf
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMessage {
    /**
     * 内部通讯使用，如果是对内使用则不会生成.proto代码
     * @return false代表对外使用
     */
    boolean privately() default false;

    /**
     * 期望的消息的发送方
     * @return false代表该消息由客户端发送，true代表是由服务器发送
     */
    boolean resp() default false;

    /**
     * 类型号，如果仅仅只是作为消息结构则使用该字段的默认值
     * @return 类型号
     */
    int messageType() default 0;

    /**
     * 命令号，如果仅仅只是作为消息结构则使用该字段的默认值
     * @return 命令号
     */
    int cmd() default 0;

    /**
     * 消息说明，如果为非空字符串，则会出现在.proto的注释中
     */
    String desc() default "";
}
