package com.tuner.recording_manager;

import com.google.common.collect.Range;
import com.tuner.recorder.StreamRecorder;
import com.tuner.utils.SchedulingUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.TreeSet;

import static com.tuner.utils.SchedulingUtils.getDate;

@Component
public class RecorderManager {
    private static final Logger logger = LoggerFactory.getLogger(RecorderManager.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${recording.fileSizeLoggingIntervalInSeconds}")
    int interval;
    @Autowired
    StreamRecorder recorder;
    @Autowired
    Scheduler scheduler;

    JobDetail sizePollingJob = null;
    JobDetail stopJob = null;
    boolean recording = false;
    TreeSet<RecordingOrder> recordingOrders = new TreeSet<>(Comparator.comparing(o -> o.start));

    public void record(RecordingOrder recordingOrder) { //TODO: return result
        if (collides(recordingOrder)) {
            logger.warn("Trying to schedule recording during already planned recording");
            return;
        }

        String id = recordingOrder.getId();
        Trigger startTrigger = SchedulingUtils.getOneRunTrigger(getDate(recordingOrder.getStart()), "startRecordingTrigger" + id);
        schedule(SchedulingUtils.getJobDetail("startRecordingJob" + id, StartRecordingJob.class), startTrigger);

        recordingOrders.add(recordingOrder);

        logger.info(String.format("Scheduled recording from: %s to: %s", recordingOrder.getStart().format(formatter), recordingOrder.getFinish().format(formatter)));
    }

    public void stop() { //TODO: return result
        recorder.stop();
        if (!recording) {
            logger.warn("trying to stop when state is marked as not recording");
            return;
        }
        cancelJob(stopJob);
        cancelJob(sizePollingJob);
        sizePollingJob = null;
    }

    private void start() {
        var order = recordingOrders.pollFirst(); // TODO: taking from tree is probably not the most roboust solution, probably job should ahve in itself - maybe replace quartz with somathong else?
        recorder.start(order.filename, order.url);

        Duration duration = Duration.between(order.start, order.finish);
        logger.info(String.format("Commanded recording for %d:%02d:%02d", duration.toSeconds() / 3600, (duration.toSeconds() % 3600) / 60, (duration.toSeconds() % 60)));

        Trigger stopTrigger = SchedulingUtils.getOneRunTrigger(getDate(order.finish), "stopRecordingTrigger");
        stopJob = schedule(SchedulingUtils.getJobDetail("stopRecordingJob", StopRecordingJob.class), stopTrigger);

        Trigger sizePollingTrigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "sizePollingRecordingTrigger");
        sizePollingJob = schedule(SchedulingUtils.getJobDetail("sizePollingRecordingJob", FileSizePollJob.class), sizePollingTrigger);

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

    private void cancelJob(JobDetail job) {
        try {
            scheduler.deleteJob(job.getKey());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private boolean collides(RecordingOrder recordingOrder) {
        var newRange = Range.closed(recordingOrder.start, recordingOrder.finish);
        return recordingOrders.stream().map(o -> Range.closed(o.start, o.finish)).anyMatch(r -> r.isConnected(newRange));
    }

    private class StartRecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            start();
        }
    }

    private class StopRecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            stop();
        }
    }

    private class FileSizePollJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            logger.info(String.format("Recording. File size: %d MB", recorder.getSize() / 1000000));
        }
    }
}
