package org.alan.mars.executor;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.utils.RandomUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 集群业务线程池
 * <p>
 * Created on 2019/7/30.
 *
 * @author Alan
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class MarsWorkExecutor {

    private final NodeConfig nodeConfig;

    private final Map<Integer, TaskGroup> executorServiceMap = new ConcurrentHashMap<>();
    private final ExecutorService threadPoolExecutor = ThreadUtil.newExecutor(Runtime.getRuntime().availableProcessors() * 16);

    public void submit(int id, Runnable work) {
        getTaskGroup(id).submit(work);
    }

    public void submit(Runnable work) {
        submit(RandomUtil.random(3000), work);
    }

    public TaskGroup newTaskGroup() {
        return new TaskGroup(threadPoolExecutor);
    }

    private TaskGroup getTaskGroup(int id) {
        int key = id % nodeConfig.workPoolNum;
        return executorServiceMap.computeIfAbsent(key, k -> newTaskGroup());
    }
}
