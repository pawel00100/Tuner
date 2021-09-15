package com.tuner.utils.scheduling;

import org.quartz.*;

import java.time.Duration;
import java.util.Date;

public class SchedulingUtils {

    public static JobDetail getJobDetail(String jobName, Class<? extends Job> job) {
        return JobBuilder.newJob(job)
                .withIdentity(jobName)
                .build();
    }

    public static Trigger getOneRunTrigger(Date date, String triggerName) {
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerName)
                .startAt(date)
                .build();
    }

    public static Trigger getScheduledTrigger(Duration interval, String triggerName) {
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerName)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds((int) interval.toSeconds())
                        .repeatForever())
                .build();
    }


    public static JobDetail schedule(Scheduler scheduler, JobDetail job, Trigger trigger) {
        try {
            scheduler.scheduleJob(job, trigger);
            return job;
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JobDetail schedule(Scheduler scheduler, JobAndTrigger jobAndTrigger) {
        return schedule(scheduler, jobAndTrigger.jobDetail(), jobAndTrigger.trigger());
    }

    public static JobAndTrigger createJobAndTrigger(String jobName, String triggerName, Class<? extends Job> job, Duration duration) {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(duration, triggerName);
        JobDetail jobDetail = SchedulingUtils.getJobDetail(jobName, job);
        return new JobAndTrigger(jobDetail, trigger);
    }

    public static JobAndTrigger createJobAndTrigger(String jobName, String triggerName, Class<? extends Job> job, Date date) {
        Trigger trigger = SchedulingUtils.getOneRunTrigger(date, triggerName);
        JobDetail jobDetail = SchedulingUtils.getJobDetail(jobName, job);
        return new JobAndTrigger(jobDetail, trigger);
    }

    public static void cancelJob(Scheduler scheduler, JobAndTrigger jobAndTrigger) {
        try {
            scheduler.unscheduleJob(jobAndTrigger.trigger().getKey());
            scheduler.deleteJob(jobAndTrigger.jobDetail().getKey());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }


}
