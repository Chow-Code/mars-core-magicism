package org.alan.mars.timer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2017/3/15.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class TimerEvent<T> implements Runnable {
    /**
     * 定义无限循环常量
     */
    public static final int INFINITE_CYCLE = 0x7fffffff;

    public static final int DEBUG_TIME = 1000;

    /* fields */
    /**
     * 定时事件监听器
     */
    protected final TimerListener<T> listener;

    /**
     * 事件动作参数
     * -- GETTER --
     *  获得事件动作参数

     */
    @Getter
    protected T parameter;

    /**
     * 间隔时间
     * -- GETTER --
     *  获得定时时间

     */
    @Getter
    protected int intervalTime;

    /**
     * 间隔时间单位
     */
    protected TimeUnit timeUnit;

    /* 业务ID，如果设置，将使用该ID进行线程分配 */
    protected int workId;

    /**
     * 定时次数
     * -- GETTER --
     *  获得定时次数

     */
    @Getter
    protected int count;

    /**
     * 初始延迟时间
     * -- GETTER --
     *  获得定时的起始延时时间

     */
    @Getter
    protected int initTime;

    /**
     * 决对时间定时，线程工作的时间计算在定时时间内
     * -- GETTER --
     *  判断是否为决对时间定时

     */
    @Getter
    protected boolean absolute;

    /**
     * 起始时间
     * -- GETTER --
     *  获得起始时间

     */
    @Getter
    protected long startTime;

    /**
     * 当前运行的时间
     * -- GETTER --
     *  获得当前运行的时间

     */
    @Getter
    protected long currentTime;

    /**
     * 下一次运行的时间
     * -- GETTER --
     *  获得下一次运行的时间

     */
    @Getter
    protected long nextTime;

    /**
     * 是否在 fire 中
     */
    protected volatile boolean inFire;

    /**
     * 是否启动或者禁用
     */
    protected volatile boolean enabled;

    /* constructors */

    protected TimerEvent(TimerListener<T> listener, T parameter) {
        this.listener = listener;
        this.parameter = parameter;
    }

    /**
     * 构造一个单次循环的定时时间
     *
     * @param listener  监听器
     * @param initTime  初始等待时间
     * @param parameter 源
     */
    public TimerEvent(TimerListener<T> listener, int initTime, T parameter) {
        this(listener, parameter, 0, 1, initTime, false);
    }

    /**
     * 构造一个定时事件对象，默认为无初始延迟时间、无限循环
     */
    public TimerEvent(TimerListener<T> listener, T parameter, int intervalTime,
                      boolean absolute) {
        this(listener, parameter, intervalTime, INFINITE_CYCLE, 0, absolute);
    }

    /**
     * 构造一个定时事件对象，默认为无初始延迟时间、无限循环，相对时间定时
     */
    public TimerEvent(TimerListener<T> listener, T parameter, int intervalTime) {
        this(listener, parameter, intervalTime, INFINITE_CYCLE, 0, false);
    }

    /**
     * 构造一个定时事件对象，默认为无初始延迟时间，相对时间定时
     */
    public TimerEvent(TimerListener<T> listener, T parameter, int intervalTime,
                      int count) {
        this(listener, parameter, intervalTime, count, 0, false);
    }

    /**
     * 构造一个定时事件对象，默认为无初始延迟时间
     */
    public TimerEvent(TimerListener<T> listener, T parameter, int intervalTime,
                      int count, boolean absolute) {
        this(listener, parameter, intervalTime, count, 0, absolute);
    }

    /**
     * 构造一个定时事件对象，默认为相对时间定时
     */
    public TimerEvent(TimerListener<T> listener, T parameter, int intervalTime,
                      int count, int initTime) {
        this(listener, parameter, intervalTime, count, initTime, false);
    }

    /**
     * 构造一个定时事件对象，设置为指定时间开始 , 若设置时间小于当前时间则立刻开始
     */
    public TimerEvent(TimerListener<T> listener, long time, T parameter) {
        this.listener = listener;
        this.nextTime = time;
        this.parameter = parameter;
        this.count = 1;
    }

    /**
     * 构造一个定时源事件对象
     *
     * @param listener     定时事件监听器
     * @param parameter    事件动作参数
     * @param intervalTime 定时时间
     * @param count        定时次数
     * @param initTime     初始延迟时间
     */
    public TimerEvent(TimerListener<T> listener, T parameter, int intervalTime,
                      int count, int initTime, boolean absolute) {
        this.listener = listener;
        this.parameter = parameter;
        this.intervalTime = intervalTime;
        this.count = count;
        this.initTime = initTime;
        this.absolute = absolute;
        this.inFire = false;
        this.enabled = true;
    }

    public TimerEvent<T> withWork(int workId) {
        this.workId = workId;
        return this;
    }

    public TimerEvent<T> withTimeUnit(TimeUnit timeUnit) {
        assert timeUnit != null;
        this.timeUnit = timeUnit;
        this.intervalTime = (int) timeUnit.toMillis(intervalTime);
        this.initTime = (int) timeUnit.toMillis(initTime);
        return this;
    }

    /* properties */

    /**
     * 获得定时事件监听器
     */
    public TimerListener<T> getTimerListener() {
        return listener;
    }

    /**
     * 设置事件动作参数
     */
    public TimerEvent<T> setParameter(T parameter) {
        this.parameter = parameter;
        return this;
    }

    /**
     * 设置定时时间
     */
    public TimerEvent<T> setIntervalTime(int time) {
        intervalTime = time;
        return this;
    }

    /**
     * 设置定时次数
     */
    public TimerEvent<T> setCount(int count) {
        this.count = count;
        return this;
    }

    /**
     * 设置是否启动或禁用
     */
    public TimerEvent<T> setEnabled(boolean b) {
        enabled = b;
        return this;
    }

    /**
     * 获取是否启用或禁用
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置定时的起始延时时间
     */
    public TimerEvent<T> setInitTime(int initTime) {
        this.initTime = initTime;
        return this;
    }

    /**
     * 设置决对或相对时间定时
     */
    public TimerEvent<T> setAbsolute(boolean b) {
        absolute = b;
        return this;
    }

    /**
     * 设置下一次运行的时间
     */
    public void setNextTime(long time) {
        nextTime = time;
    }

    /* methods */

    /**
     * 初始化方法
     */
    public void init() {
        startTime = System.currentTimeMillis();
        enabled = true;
        if (nextTime == 0) {
            nextTime = startTime + initTime;
        }
    }

    /**
     * 引发定时事件，通知定时事件监听器，设置定时次数和下一次的运行时间
     */
    void fire(long currentTime) {
        if (count != INFINITE_CYCLE)
            count--;
        this.currentTime = currentTime;
        nextTime = absolute ? (nextTime + intervalTime)
                : (currentTime + intervalTime);
        try {
            listener.onTimer(this);
        } catch (Throwable e) {
            if (log.isWarnEnabled())
                log.warn("fire error, " + this, e);
        }
        long endTime = System.currentTimeMillis();

        // 添加对执行超过1秒的事件进行打印
        if (log.isDebugEnabled()) {
            if (endTime - currentTime > DEBUG_TIME) {
                log.warn("event fire long time,event=" + this);
            }
        }
        inFire = false;
    }

    /* common methods */
    public String toString() {
        return super.toString() + "[listener=" + listener + ", parameter="
                + parameter + ", intervalTime=" + intervalTime + ", count="
                + count + ", initTime=" + initTime + ", absolute=" + absolute
                + "]";
    }

    @Override
    public void run() {
        fire(System.currentTimeMillis());
    }
}
