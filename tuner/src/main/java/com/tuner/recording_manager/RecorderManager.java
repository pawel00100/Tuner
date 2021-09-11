package com.tuner.recording_manager;

import com.google.common.collect.Range;
import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.model.server_requests.Channel;
import com.tuner.model.server_requests.RecordedFile;
import com.tuner.recorded_files.RecordListProvider;
import com.tuner.recorder.Recorder;
import com.tuner.utils.SchedulingUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tuner.utils.TimezoneUtils.*;

@Component
public class RecorderManager {
    private static final Logger logger = LoggerFactory.getLogger(RecorderManager.class);
    @Value("${recording.fileSizeLoggingIntervalInSeconds}")
    private static final int interval = 10;
    private final TreeSet<ScheduledOrder> recordingOrders = new TreeSet<>(Comparator.comparing(o -> o.order().getStart()));
    private final Recorder recorder;
    private final RecordListProvider recordListProvider;
    private final Scheduler scheduler;
    private final ChannelProvider channelProvider;

    private JobAndTrigger sizePollingJob = null;
    private JobAndTrigger stopJob = null;
    private boolean recording = false;
    private RecordingOrderInternal currentOrder = null;

    @Autowired
    public RecorderManager(Recorder recorder, RecordListProvider recordListProvider, Scheduler scheduler, ChannelProvider channelProvider) {
        this.recorder = recorder;
        this.recordListProvider = recordListProvider;
        this.scheduler = scheduler;
        this.channelProvider = channelProvider;
    }

    public void scheduleRecording(List<RecordingOrderInternal> newOrders) {
        var toRemove = recordingOrders.stream()
                .filter(o -> o.order().isFromServer())
                .filter(o -> !newOrders.contains(o.order()))
                .collect(Collectors.toList());

        if (recording && !recordingOrders.isEmpty() && toRemove.contains(recordingOrders.first())) {
            toRemove.remove(recordingOrders.first());
            stop();
        }

        toRemove.forEach(this::cancelJob);
        newOrders.forEach(this::scheduleRecording);
    }

    public void scheduleRecording(RecordingOrderInternal recordingOrder) { //TODO: return result
        if (identical(recordingOrder)) {
            logger.trace("Skipping identical recording");
            return;
        }
        if (collides(recordingOrder)) {
            logger.warn("Trying to schedule recording during already planned recording");
            return;
        }

        if (recordingOrder.getStart().toEpochSecond() * 1000 <= System.currentTimeMillis()) {
            recordingOrder.setStart(ZonedDateTime.now());
        }

        String id = recordingOrder.getId();
        Trigger startTrigger = SchedulingUtils.getOneRunTrigger(getDate(recordingOrder.getStart()), "startRecordingTrigger" + id);
        JobDetail jobDetail = SchedulingUtils.getJobDetail("startRecordingJob" + id, StartRecordingJob.class);

        recordingOrders.add(new ScheduledOrder(recordingOrder, jobDetail, startTrigger));

        schedule(jobDetail, startTrigger);

        logger.info("Scheduled recording from: {} to: {}", formattedAtLocal(recordingOrder.getStart()), formattedAtLocal(recordingOrder.getEnd()));
    }

    public void stop() { //TODO: return result
        recorder.stop();
        if (!recording) {
            logger.warn("trying to stop when state is marked as not recording");
            return;
        }
        cancelJob(stopJob);
        cancelJob(sizePollingJob);
        recordListProvider.registerRecording(new RecordedFile(currentOrder, recorder.recordingTimeInSeconds(), recorder.getSize()));
        recording = false;
        recordingOrders.pollFirst();
        sizePollingJob = null;
    }

    private void start() {
        var order = recordingOrders.first().order(); // TODO: taking from tree is probably not the most roboust solution, probably job should ahve in itself - maybe replace quartz with something else?
        String filename = createFilename(order.getChannel(), order.getStart());
        order.setFilename(filename);
        recorder.start(filename, order.getChannel().getId());

        logger.info("Commanded recording until {}", formattedAtLocal(order.getEnd()));

        stopJob = createJobAndTrigger("stopRecordingJob", "stopRecordingTrigger", StopRecordingJob.class, getDate(order.getEnd()));
        schedule(stopJob);

        sizePollingJob = createJobAndTrigger("sizePollingRecordingJob", "sizePollingRecordingTrigger", FileSizePollJob.class, Duration.ofSeconds(interval));
        schedule(sizePollingJob);

        currentOrder = order;
        recording = true;
    }

    private JobDetail schedule(JobDetail job, Trigger trigger) {
        try {
            scheduler.scheduleJob(job, trigger);
            return job;
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JobDetail schedule(JobAndTrigger jobAndTrigger) {
        return schedule(jobAndTrigger.jobDetail(), jobAndTrigger.trigger());
    }

    private void cancelJob(JobAndTrigger jobAndTrigger) {
        try {
            scheduler.unscheduleJob(jobAndTrigger.trigger().getKey());
            scheduler.deleteJob(jobAndTrigger.jobDetail().getKey());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private void cancelJob(ScheduledOrder order) {
        try {
            scheduler.unscheduleJob(order.trigger().getKey());
            scheduler.deleteJob(order.jobDetail().getKey());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        recordingOrders.remove(order);
    }


    private boolean collides(RecordingOrderInternal recordingOrder) {
        var newRange = Range.closed(recordingOrder.getStart(), recordingOrder.getEnd());
        return recordingOrders.stream()
                .map(o -> Range.closed(o.order().getStart(), o.order().getEnd()))
                .anyMatch(r -> r.isConnected(newRange));
    }

    private boolean identical(RecordingOrderInternal recordingOrder) {
        var potential = recordingOrders.ceiling(new ScheduledOrder(recordingOrder, null, null));

        if (potential == null) {
            return false;
        }
        return recordingOrder.getEnd().equals(potential.order().getEnd()) &&
                recordingOrder.getChannel().equals(potential.order().getChannel());
    }

    private String createFilename(Channel channel, ZonedDateTime start) {
        return channel.getName() + " " + formattedAtLocalForFilename(start) + ".mp4";
    }

    private JobAndTrigger createJobAndTrigger(String jobName, String triggerName, Class<? extends Job> job, Duration duration) {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(duration, triggerName);
        JobDetail jobDetail = SchedulingUtils.getJobDetail(jobName, job);
        return new JobAndTrigger(jobDetail, trigger);
    }

    private JobAndTrigger createJobAndTrigger(String jobName, String triggerName, Class<? extends Job> job, Date date) {
        Trigger trigger = SchedulingUtils.getOneRunTrigger(date, triggerName);
        JobDetail jobDetail = SchedulingUtils.getJobDetail(jobName, job);
        return new JobAndTrigger(jobDetail, trigger);
    }

    private record ScheduledOrder(RecordingOrderInternal order, JobDetail jobDetail, Trigger trigger) {
    }

    private record JobAndTrigger(JobDetail jobDetail, Trigger trigger) {
    }

    public class StartRecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            start();
        }
    }

    public class StopRecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            stop();
        }
    }

    public class FileSizePollJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            logger.info("Recording. File size: {} MB", recorder.getSize() / 1000000);
        }
    }

}
