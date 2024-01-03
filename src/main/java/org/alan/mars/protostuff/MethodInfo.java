package org.alan.mars.protostuff;

import java.lang.reflect.Type;

/**
 * Created on 2017/7/28.
 *
 * @author Alan
 * @since 1.0
 */
public class MethodInfo {
    public final int index;
    public final String name;
    public final Class<?>[] params;
    public final Type returnType;

    public MethodInfo(int index, String name, Class<?>[] params, Type returnType) {
        this.index = index;
        this.name = name;
        this.params = params;
        this.returnType = returnType;
    }
}
