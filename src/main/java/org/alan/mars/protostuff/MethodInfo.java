package org.alan.mars.protostuff;

import java.lang.reflect.Type;

/**
 * Created on 2017/7/28.
 *
 * @author Alan
 * @since 1.0
 */
public class MethodInfo {
    public int index;
    public String name;
    public Class<?>[] params;
    public Type returnType;

    public MethodInfo(int index, String name, Class<?>[] params, Type returnType) {
        this.index = index;
        this.name = name;
        this.params = params;
        this.returnType = returnType;
    }
}
