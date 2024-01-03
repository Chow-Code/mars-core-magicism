package org.alan.mars.timer;

/**
 * 定时事件监听器
 * Created on 2017/3/15.
 *
 * @author Alan
 * @since 1.0
 */
public interface TimerListener<T> {
    /**
     * 定时事件的监听方法
     */
    void onTimer(TimerEvent<T> e);
}
