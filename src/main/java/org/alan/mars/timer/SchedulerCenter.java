package org.alan.mars.timer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;
import java.util.TimeZone;

/**
 * 程序调度管理器，用于各种定时任务，循环任务等
 *
 * @author Chow
 * @version 1.0
 * {@code @All} rights reserved.
 * <p>
 * <p>
 * <pre>
 * Quartz Cron 表达式支持到七个域 名称 是否必须 允许值 特殊字符 秒 是 0-59 , - * / 分 是 0-59 , - * /
 *      时 是 0-23 , - * / 日 是 1-31 , - * ? / L W C 月 是 1-12 或 JAN-DEC , - * / 周 是
 * 1-7 或 SUN-SAT , - * ? / L C # 年 否 空 或 1970-2099 , - * /
 *
 *      <pre>
 * 月份和星期的名称是不区分大小写的。FRI 和 fri 是一样的。
 *
 *      域之间有空格分隔，这和 UNIX cron 一样。无可争辩的，我们能写的最简单的表达式看起来就是这个了：
 *
 *      * * ? * *
 *
 *      这个表达会每秒钟(每分种的、每小时的、每天的)激发一个部署的 job。
 *
 *      ·理解特殊字符
 *
 *      同 UNIX cron 一样，Quartz cron 表达式支持用特殊字符来创建更为复杂的执行计划。然而，Quartz 在特殊字符的支持上比标准
 *      UNIX cron 表达式更丰富了。
 *
 *      星号
 *
 *      使用星号(*) 指示着你想在这个域上包含所有合法的值。例如，在月份域上使用星号意味着每个月都会触发这个 trigger。
 *
 *      表达式样例：
 *
 *      0 * 17 * * ?
 *
 *      意义：每天从下午5点到下午5:59中的每分钟激发一次 trigger。它停在下午 5:59 是因为值 17 在小时域上，在下午 6
 *      点时，小时变为 18 了，也就不再理会这个 trigger，直到下一天的下午5点。
 *
 *      在你希望 trigger 在该域的所有有效值上被激发时使用 * 字符。
 *
 *      ? 问号
 *
 *      ? 号只能用在日和周域上，但是不能在这两个域上同时使用。你可以认为 ? 字符是 "我并不关心在该域上是什么值。"
 *      这不同于星号，星号是指示着该域上的每一个值。? 是说不为该域指定值。
 *
 *      不能同时这两个域上指定值的理由是难以解释甚至是难以理解的。基本上，假定同时指定值的话，意义就会变得含混不清了：考虑一下，
 *      如果一个表达式在日域上有值11，同时在周域上指定了 WED。那么是要 trigger 仅在每个月的11号，
 *      且正好又是星期三那天被激发？还是在每个星期三的11号被激发呢？要去除这种不明确性的办法就是不能同时在这两个域上指定值。
 *
 *      只要记住，假如你为这两域的其中一个指定了值，那就必须在另一个字值上放一个 ?。
 *
 *      表达式样例：
 *
 *      0 10,44 14 ? 3 WEB
 *
 *      意义：在三月中的每个星期三的下午 2:10 和 下午 2:44 被触发。
 *
 *      , 逗号
 *
 *      逗号 (,) 是用来在给某个域上指定一个值列表的。例如，使用值 0,15,30,45 在秒域上意味着每15秒触发一个 trigger。
 *
 *      表达式样例：
 *
 *      0 0,15,30,45 * * * ?
 *
 *      意义：每刻钟触发一次 trigger。
 *
 *      / 斜杠
 *
 *      斜杠 (/) 是用于时间表的递增的。我们刚刚用了逗号来表示每15分钟的递增，但是我们也能写成这样 0/15。
 *
 *      表达式样例：
 *
 *      0/15 0/30 * * * ?
 *
 *      意义：在整点和半点时每15秒触发 trigger。
 *
 *      - 中划线
 *
 *      中划线 (-) 用于指定一个范围。例如，在小时域上的 3-8 意味着 "3,4,5,6,7 和 8 点。" 域的值不允许回卷，所以像 50-10
 *      这样的值是不允许的。
 *
 *      表达式样例：
 *
 *      0 45 3-8 ? * *
 *
 *      意义：在上午的3点至上午的8点的45分时触发 trigger。
 *
 *      L 字母
 *
 *      L 说明了某域上允许的最后一个值。它仅被日和周域支持。当用在日域上，表示的是在月域上指定的月份的最后一天。例如，当月域上指定了 JAN
 *      时，在日域上的 L 会促使 trigger 在1月31号被触发。假如月域上是 SEP，那么 L
 *      会预示着在9月30号触发。换句话说，就是不管指定了哪个月，都是在相应月份的时最后一天触发 trigger。
 *
 *      表达式 0 0 8 L * ? 意义是在每个月最后一天的上午 8:00 触发 trigger。在月域上的 * 说明是 "每个月"。
 *
 *      当 L 字母用于周域上，指示着周的最后一天，就是星期六 (或者数字7)。所以如果你需要在每个月的最后一个星期六下午的 11:59 触发
 *      trigger，你可以用这样的表达式 0 59 23 ? * L。
 *
 *      当使用于周域上，你可以用一个数字与 L 连起来表示月份的最后一个星期 X。例如，表达式 0 0 12 ? * 2L
 *      说的是在每个月的最后一个星期一触发 trigger。
 *
 *      不要让范围和列表值与 L 连用
 *
 *      虽然你能用星期数(1-7)与 L 连用，但是不允许你用一个范围值和列表值与 L 连用。这会产生不可预知的结果。
 *
 *      W 字母
 *
 *      W 字符代表着平日 (Mon-Fri)，并且仅能用于日域中。它用来指定离指定日的最近的一个平日。大部分的商业处理都是基于工作周的，所以 W
 *      字符可能是非常重要的。例如，日域中的 15W 意味着 "离该月15号的最近一个平日。" 假如15号是星期六，那么 trigger 会在14号
 *      (星期四)触发，因为距15号最近的是星期一，这个例子中也会是17号（注：不会在17号触发的，如果是15W，可能会是在14号
 *      (15号是星期六)或者15号(15号是星期天)触发，也就是只能出现在邻近的一天，如果15号当天为平日直接就会当日执行）。W
 *      只能用在指定的日域为单天，不能是范围或列表值。
 *
 *      # 井号
 *
 *      # 字符仅能用于周域中。它用于指定月份中的第几周的哪一天。例如，如果你指定周域的值为 6#3，它意思是某月的第三个周五
 *      (6=星期五，#3意味着月份中的第三周)。另一个例子 2#1 意思是某月的第一个星期一
 *      (2=星期一，#1意味着月份中的第一周)。注意，假如你指定 #5，然而月份中没有第 5 周，那么该月不会触发。
 *
 *
 *      Cron 表达式 Cookbook
 *
 *      此处的 Cron 表达式 cookbook
 *      旨在为常用地执行需求提供方案。尽管不可能列举出所有的表达式，但下面的应该为满足你的业务需求提供了足够的例子。
 *
 *      ·分钟的 Cron 表达式
 *
 *      表 5.1. 包括了分钟频度的任务计划 Cron 表达式 用法 表达式 每天的从 5:00 PM 至 5:59 PM 中的每分钟触发 0 *
 *      17 * * ?
 *
 *      每天的从 11:00 PM 至 11:55 PM 中的每五分钟触发 0 0/5 23 * * ?
 *
 *      每天的从 3:00 至 3:55 PM 和 6:00 PM 至 6:55 PM 之中的每五分钟触发 0 0/5 15,18 * * ?
 *
 *      每天的从 5:00 AM 至 5:05 AM 中的每分钟触发 0 0-5 5 * * ?
 *
 *
 *      ·日上的 Cron 表达式
 *
 *      表 5.2. 基于日的频度上任务计划的 Cron 表达式 用法 表达式 每天的 3:00 AM 0 0 3 * * ? 每天的 3:00 AM
 *      (另一种写法) 0 0 3 ? * * 每天的 12:00 PM (中午) 0 0 12 * * ? 在 2005 中每天的 10:15 AM
 *      0 15 10 * * ? 2005
 *
 *      ·周和月的 Cron 表达式
 *
 *      表 5.3. 基于周和/或月的频度上任务计划的 Cron 表达式 用法 表达式 在每个周一,二, 三和周四的 10:15 AM 0 15 10
 *      ? * MON-FRI 每月15号的 10:15 AM 0 15 10 15 * ? 每月最后一天的 10:15 AM 0 15 10 L *
 *      ? 每月最后一个周五的 10:15 AM 0 15 10 ? * 6L 在 2002, 2003, 2004, 和 2005
 *      年中的每月最后一个周五的 10:15 AM 0 15 10 ? * 6L 2002-2005 每月第三个周五的 10:15 AM 0 15 10
 *      ? * 6#3 每月从第一天算起每五天的 12:00 PM (中午) 0 0 12 1/5 * ? 每一个 11 月 11 号的 11:11
 * AM 0 11 11 11 11 ? 三月份每个周三的 2:10 PM 和 2:44 PM 0 10,44 14 ? 3 WED
 */
@Slf4j
public class SchedulerCenter {

    private final SchedulerFactory schedulerFactory;
    /**
     * 默认是机器的时区
     */
    private final TimeZone timezone;

    @Getter
    private volatile boolean start;

    public SchedulerCenter() throws Exception {
        this(TimeZone.getDefault());
    }

    public SchedulerCenter(TimeZone timezone) throws Exception {
        this(null, timezone);
    }

    public SchedulerCenter(String configFile, TimeZone timezone) throws Exception {
        if (StrUtil.isBlank(configFile)) {
            schedulerFactory = new StdSchedulerFactory();
        } else {
            schedulerFactory = new StdSchedulerFactory(configFile);
        }
        this.timezone = timezone;
    }

    public void start() {
        this.start = true;
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.start();
        } catch (Exception e) {
            log.error("开启计划任务失败", e);
        }
    }

    public void stop() {
        this.start = false;
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.shutdown();
        } catch (Exception e) {
            log.error("关闭计划任务失败", e);
        }
    }

    public void addJob(SchedulerEvent<?> schedulerEvent) {
        try {
            String cron = schedulerEvent.getCronExpression();
            JobDetail job = JobBuilder.newJob(JobDemo.class).build();
            job.getJobDataMap().put(SchedulerEvent.class.getSimpleName(), schedulerEvent);
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron).inTimeZone(timezone))
                    .build();
            schedulerEvent.setJobKey(job.getKey());
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.scheduleJob(job, trigger);
            if (start) {
                Date date = trigger.getNextFireTime();
                schedulerEvent.nextTime = date.getTime();
                log.info("加入新的计划任务, cron = {}, 下次执行时间 = {}({})", cron, DateUtil.format(new Date(schedulerEvent.nextTime),"yyyy-MM-dd HH:mm:ss"),schedulerEvent.nextTime);
            } else {
                log.info("加入新的计划任务, cron = {}, 将在启动过后才执行", cron);
            }
        } catch (Exception e) {
            log.error("加入计划任务失败", e);
        }
    }

    public void removeJobs(SchedulerEvent<?> schedulerEvent) {
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            scheduler.deleteJob(schedulerEvent.getJobKey());
        } catch (SchedulerException e) {
            log.warn("", e);
        }
    }

    public static class JobDemo implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext)
                throws JobExecutionException {
            JobDetail jobDetail = jobExecutionContext.getJobDetail();
            SchedulerEvent<?> event = (SchedulerEvent<?>) jobDetail.getJobDataMap().get(SchedulerEvent.class.getSimpleName());
            event.execute(jobExecutionContext);
            if (jobExecutionContext.getNextFireTime() == null) {
                event.nextTime = -1;
            } else {
                event.nextTime = jobExecutionContext.getNextFireTime().getTime();
            }
        }
    }
}
