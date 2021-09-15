package com.tuner.utils.scheduling;

import org.quartz.JobDetail;
import org.quartz.Trigger;

public record JobAndTrigger(JobDetail jobDetail, Trigger trigger) {
}