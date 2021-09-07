package com.tuner.recording_manager;

import com.google.common.collect.Range;
import com.tuner.connector_to_tvh.ChannelProvider;
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
import java.util.List;
import java.util.TreeSet;

import static com.tuner.utils.TimezoneUtils.*;

@Component
public class RecorderManager {
    private static final Logger logger = LoggerFactory.getLogger(RecorderManager.class);

    @Value("${recording.fileSizeLoggingIntervalInSeconds}")
    int interval;
    @Autowired
    Recorder recorder;
    @Autowired
    RecordListProvider recordListProvider;
    private final TreeSet<RecordingOrderInternal> recordingOrders = new TreeSet<>(Comparator.comparing(o -> o.getStart()));
    @Autowired
    Scheduler scheduler;
    @Autowired
    ChannelProvider channelProvider;
    private JobDetail sizePollingJob = null;
    private JobDetail stopJob = null;
    private boolean recording = false;
    private RecordingOrderInternal currentOrder = null;

    public void record(List<RecordingOrderInternal> newOrders) {
        var toRemove = recordingOrders.stream()
                .filter(RecordingOrderInternal::isFromServer)
                .filter(o -> !newOrders.contains(o))
                .toList();
        recordingOrders.removeAll(toRemove);
        newOrders.forEach(this::record);
    }

    public void record(RecordingOrderInternal recordingOrder) { //TODO: return result
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
            recordingOrders.add(recordingOrder);
            logger.info(String.format("Scheduled recording from: now to: %s", formattedAtLocal(recordingOrder.getEnd())));
            start();
            return;
        }

        String id = recordingOrder.getId();
        Trigger startTrigger = SchedulingUtils.getOneRunTrigger(getDate(recordingOrder.getStart()), "startRecordingTrigger" + id);
        schedule(SchedulingUtils.getJobDetail("startRecordingJob" + id, StartRecordingJob.class), startTrigger);

        recordingOrders.add(recordingOrder);

        logger.info(String.format("Scheduled recording from: %s to: %s", formattedAtLocal(recordingOrder.getStart()), formattedAtLocal(recordingOrder.getEnd())));
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
        var order = recordingOrders.first(); // TODO: taking from tree is probably not the most roboust solution, probably job should ahve in itself - maybe replace quartz with something else?
        String filename = createFilename(order.getChannelId(), order.getStart());
        order.setFilename(filename);
        recorder.start(filename, order.getChannelId());

        Duration duration = Duration.between(order.getStart(), order.getEnd());
        logger.info(String.format("Commanded recording for %d:%02d:%02d", duration.toSeconds() / 3600, (duration.toSeconds() % 3600) / 60, (duration.toSeconds() % 60)));

        Trigger stopTrigger = SchedulingUtils.getOneRunTrigger(getDate(order.getEnd()), "stopRecordingTrigger");
        stopJob = schedule(SchedulingUtils.getJobDetail("stopRecordingJob", StopRecordingJob.class), stopTrigger);

        Trigger sizePollingTrigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "sizePollingRecordingTrigger");
        sizePollingJob = schedule(SchedulingUtils.getJobDetail("sizePollingRecordingJob", FileSizePollJob.class), sizePollingTrigger);

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

    private void cancelJob(JobDetail job) {
        try {
            scheduler.deleteJob(job.getKey());
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private boolean collides(RecordingOrderInternal recordingOrder) {
        var newRange = Range.closed(recordingOrder.getStart(), recordingOrder.getEnd());
        return recordingOrders.stream().map(o -> Range.closed(o.getStart(), o.getEnd())).anyMatch(r -> r.isConnected(newRange));
    }

    private boolean identical(RecordingOrderInternal recordingOrder) {
        var potential = recordingOrders.ceiling(recordingOrder);

        if (potential == null) {
            return false;
        }
        return recordingOrder.getEnd().equals(potential.getEnd()) && recordingOrder.getChannelId().equals(potential.getChannelId());
    }

    private String createFilename(String channel, ZonedDateTime start) {
        return channelProvider.getName(channel) + " " + formattedAtLocalForFilename(start) + ".mp4";
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
