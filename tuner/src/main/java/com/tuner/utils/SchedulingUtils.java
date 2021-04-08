package com.tuner.utils;

import org.quartz.*;

import java.time.Duration;
import java.time.LocalDateTime;
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

    public static Date getDate(Duration duration) {
        return DateBuilder.futureDate((int) duration.toSeconds(), DateBuilder.IntervalUnit.SECOND);
    }

    public static Date getDate(LocalDateTime time) {
        return java.sql.Timestamp.valueOf(time);
    }
}
