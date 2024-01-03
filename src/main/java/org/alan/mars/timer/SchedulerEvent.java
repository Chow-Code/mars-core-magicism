package org.alan.mars.timer;

import lombok.Getter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

/**
 * Created on 2017/4/19.
 *
 * @author Alan
 * @since 1.0
 */
@Getter
public class SchedulerEvent<T> extends TimerEvent<T> implements Job {
    private final String cronExpression;

    private JobExecutionContext jobExecutionContext;

    private JobKey jobKey;

    public SchedulerEvent(TimerListener<T> listener, T parameter, String cronExpression) {
        super(listener, parameter);
        this.cronExpression = cronExpression;
        this.jobKey = new JobKey(listener.getClass().getName() + "_" + cronExpression);
    }

    @Override
    public void execute(JobExecutionContext context) {
        listener.onTimer(this);
    }

    public void setJobExecutionContext(JobExecutionContext jobExecutionContext) {
        this.jobExecutionContext = jobExecutionContext;
    }

    public void setJobKey(JobKey jobKey) {
        this.jobKey = jobKey;
    }

}
