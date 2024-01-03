package org.alan.mars;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public final class MarsContext implements ApplicationContextAware {
    private static ApplicationContext CONTEXT;
    public static ApplicationContext getContext() {
        return CONTEXT;
    }
    public static final String DEV_PROFILE = "dev";
    public static final String TEST_PROFILE = "test";
    public static final String RELEASE_PROFILE = "release";

    public static boolean isDev() {
        String[] activeProfiles = getContext().getEnvironment().getActiveProfiles();
        return Arrays.asList(activeProfiles).contains(DEV_PROFILE);
    }

    public static boolean isTest() {
        String[] activeProfiles = getContext().getEnvironment().getActiveProfiles();
        return Arrays.asList(activeProfiles).contains(TEST_PROFILE);
    }

    public static boolean isRelease() {
        String[] activeProfiles = getContext().getEnvironment().getActiveProfiles();
        return Arrays.asList(activeProfiles).contains(RELEASE_PROFILE);
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
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
            throws BeansException {
        CONTEXT = applicationContext;
    }
}
