/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.timer;

import lombok.extern.slf4j.Slf4j;
import org.alan.mars.executor.MarsWorkExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;

/**
 * Created on 2017/3/15.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class TimerCenter extends Thread {

    /**
     * 定时器事件数组
     */
    private List<TimerEvent<?>> array = new CopyOnWriteArrayList<>();
    /**
     * 活动标志
     */
    volatile boolean active;
    private long lastCollateTime = 0L;
    /**
     * 定时事件执行线程池
     */
    private ThreadPoolExecutor timerService;
    /* qz 类型的时间表类型定时器*/
    private SchedulerCenter schedulerCenter;
    /* 线性业务执行器*/
    @Autowired(required = false)
    private MarsWorkExecutor workExecutor;

    public TimerCenter(String timerName) {
        super(timerName);
    }

    private void begin() {
        active = true;
        timerService = new ThreadPoolExecutor(1,
                Runtime.getRuntime().availableProcessors() + 1, 10L,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(8),
                new ThreadPoolExecutor.CallerRunsPolicy());
        schedulerCenter = new SchedulerCenter();
    }

    public SchedulerCenter schedulerCenter() {
        return this.schedulerCenter;
    }

    /**
     * 判断是否包含指定的对象
     */
    public boolean contain(TimerEvent<?> e) {
        return array.contains(e);
    }

    public List<TimerEvent<?>> getArray() {
        return array;
    }

    public ExecutorService getTimerService() {
        return timerService;
    }

    public void setArray(List<TimerEvent<?>> array) {
        this.array = array;
    }

    public void setTimerService(ThreadPoolExecutor timerService) {
        this.timerService = timerService;
    }

    /**
     * 获得指定监听器和动作参数的定时器事件（如果多个则返回第一个）
     */
    public TimerEvent<?> get(TimerListener<?> listener, Object parameter) {
        for (TimerEvent<?> ev : array) {
            if (listener != ev.getTimerListener()) {
                continue;
            }
            if (parameter == null || parameter.equals(ev.getParameter())) {
                return ev;
            }
        }
        return null;
    }

    /**
     * 加上一个定时器事件
     */
    public void add(TimerEvent<?> e) {
        array.remove(e);
        e.init();
        array.add(e);
    }

    /**
     * 移除指定的定时器事件
     */
    public void remove(TimerEvent<?> e) {
        if (e != null) {
            array.remove(e);
        }
    }

    /**
     * 移除指定定时事件监听器的定时器事件，包括所有的事件动作参数
     */
    public void remove(TimerListener<?> listener) {
        remove(listener, null);
    }

    /**
     * 移除带有指定的定时事件监听器和事件动作参数的定时器事件
     */
    public void remove(TimerListener<?> listener, Object parameter) {
        for (TimerEvent<?> ev : array) {
            if (listener != ev.getTimerListener()) {
                continue;
            }
            if (parameter == null || parameter.equals(ev.getParameter())) {
                array.remove(ev);
            }
        }
    }

    public void run() {
        begin();
        while (active) {
            try {
                long currentTime = System.currentTimeMillis();
                fire(currentTime);
                int collateInterval = 60 * 1000;
                if (currentTime - lastCollateTime > collateInterval) {
                    collate();
                    lastCollateTime = currentTime;
                }
                int runTime = 10;
                sleep(runTime);
            } catch (Exception e) {
                log.warn("", e);
            }
        }
    }

    /**
     * 通知所有定时器事件，检查是否要引发定时事件
     */
    public void fire(long time) {
        for (TimerEvent<?> ev : array) {
            if (ev == null || ev.count <= 0 || !ev.getEnabled()) {
                array.remove(ev);
            } else if (time >= ev.nextTime) {
                ev.inFire = true;
                if (workExecutor != null && ev.workId > 0) {
                    workExecutor.submit(ev.workId, ev);
                } else {
                    timerService.execute(ev);
                }
            }
        }
    }

    public void collate() {
        // 获得当前线程池中执行的任务队列长度
        int queueSize = timerService.getQueue().size();
        // 获得当前线程池中的线程数
        int poolSize = timerService.getPoolSize();
        // 获取曾经同时位于池中的最大线程数
        int largestPoolSize = timerService.getLargestPoolSize();
        // 可使用的最大线程数
        int maxPoolSize = timerService.getMaximumPoolSize();
        log.info("collate millis thread pool,queueSize=" + queueSize
                + ",poolSize=" + poolSize + ",largestPoolSize="
                + largestPoolSize + ",maxPoolSize=" + maxPoolSize);
    }

    public void close() {
        active = false;
        clear();
        timerService.shutdown();
    }

    /**
     * 清理方法
     */
    public void clear() {
        array.clear();
    }
}
