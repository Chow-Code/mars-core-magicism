package org.alan.mars.executor;

import org.alan.mars.config.NodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 集群业务线程池
 * <p>
 * Created on 2019/7/30.
 *
 * @author Alan
 * @since 1.0
 */
@Component
public class MarsWorkExecutor {
    @Autowired
    private NodeConfig nodeConfig;

    private final Map<Integer, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();

    public void submit(int id, Runnable work) {
        getExecutorService(id).submit(work);
    }

    private ExecutorService getExecutorService(int id) {
        int key = id % nodeConfig.workPoolNum;
        return executorServiceMap.computeIfAbsent(key, k -> Executors.newSingleThreadExecutor());
    }

}
