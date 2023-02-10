/**
 * Copyright Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 *
 * 2017年3月1日 	
 */
package org.alan.mars;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public final class MarsContext implements ApplicationContextAware {

    static Map<String, Object> marsBeans = new HashMap<>();

    static ApplicationContext CONTEXT;

    public static ApplicationContext getContext() {
        return CONTEXT;
    }

    public static <T> T getMarsBean(Class<T> clazz) {
        return (T) marsBeans.get(clazz.getSimpleName());
    }

    public static void addMarsBean(Object obj) {
        marsBeans.put(obj.getClass().getSimpleName(), obj);
    }

    // 通过name获取 Bean.
    public static Object getBean(String name) {
        return getContext().getBean(name);
    }

    // 通过class获取Bean.
    public static <T> T getBean(Class<T> clazz) {
        return getContext().getBean(clazz);
    }

    // 通过name,以及Clazz返回指定的Bean
    public static <T> T getBean(String name, Class<T> clazz) {
        return getContext().getBean(name, clazz);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        CONTEXT = applicationContext;
    }
}
