package org.alan.mars.protostuff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created on 2017/8/2.
 *
 * @author Alan
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufMessage {
    boolean privately() default false;

    boolean resp() default false;

    int messageType() default 0;

    int cmd() default 0;
    String desc() default "";
}
