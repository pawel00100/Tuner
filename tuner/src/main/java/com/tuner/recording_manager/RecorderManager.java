package com.tuner.recording_manager;

import com.google.common.collect.Range;
import com.tuner.model.server_requests.Channel;
import com.tuner.model.server_requests.RecordedFile;
import com.tuner.recorded_files.RecordListProvider;
import com.tuner.recorder.Recorder;
import com.tuner.utils.scheduling.JobAndTrigger;
import com.tuner.utils.scheduling.SchedulingUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.tuner.utils.TimezoneUtils.*;
import static com.tuner.utils.scheduling.SchedulingUtils.createJobAndTrigger;

@Component
public class RecorderManager {
    private static final Logger logger = LoggerFactory.getLogger(RecorderManager.class);
    private final TreeMap<String, ScheduledOrder> recordingOrders = new TreeMap<>(); //TODO: BiMap?
    private final TreeMap<String, Recording> startedOrders = new TreeMap<>(); //TODO: BiMap?
    private final RecordListProvider recordListProvider;
    private final Scheduler scheduler;
    private final ApplicationContext applicationContext;

    @Autowired
    public RecorderManager(ApplicationContext applicationContext, RecordListProvider recordListProvider, Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.recordListProvider = recordListProvider;
        this.scheduler = scheduler;
    }

    public void scheduleRecording(List<RecordingOrderInternal> newOrders) {
        var scheduledToRemove = recordingOrders.values().stream()
                .filter(o -> o.order().isFromServer())
                .filter(o -> !newOrders.contains(o.order()))
                .toList();

        var currentToRemove = startedOrders.values().stream()
                .filter(o -> o.order().isFromServer())
                .filter(o -> !newOrders.contains(o.order()))
                .toList();

        currentToRemove.stream()
                .filter(o -> startedOrders.values().stream().map(Recording::order).toList().contains(o.order()))
                .forEach(o -> stop(o.order()));

        scheduledToRemove.stream()
                .filter(o -> !startedOrders.values().stream().map(Recording::order).toList().contains(o.order()))
                .forEach(this::cancelJob);

        newOrders.stream()
                .filter(o -> !scheduledToRemove.stream().map(ScheduledOrder::order).toList().contains(o))
                .forEach(this::scheduleRecording);
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

        recordingOrders.put("startRecordingJob" + id, new ScheduledOrder(recordingOrder, jobDetail, startTrigger));

        schedule(jobDetail, startTrigger);

        logger.info("Scheduled recording from: {} to: {}", formattedAtLocal(recordingOrder.getStart()), formattedAtLocal(recordingOrder.getEnd()));
    }

    public void stop(String channel) {
        stop(channel, r -> r.order().getChannel().getId());
    }

    public void stop(RecordingOrderInternal recordingOrderInternal) {
        stop(recordingOrderInternal, Recording::order);
    }

    private <T> void stop(T t, Function<Recording, T> function) {
        var recording = startedOrders.values().stream()
                .filter(r -> function.apply(r).equals(t))
                .findAny();

        if (recording.isEmpty()) {
            logger.error("no recording found");
            return;
        }

        stop(recording.get());
    }

    private void stop(JobExecutionContext jobExecutionContext) {
        String name = jobExecutionContext.getJobDetail().getKey().getName();
        var recording = startedOrders.get(name);
        stop(recording);
    }

    private void stop(Recording rec) { //TODO: return result
        Recorder recorder = rec.recorder();
        recorder.stop();

        cancelJob(rec.stop());
        recordListProvider.registerRecording(new RecordedFile(rec.order(), recorder.recordingTimeInSeconds(), recorder.getSize()));
        startedOrders.entrySet().stream()
                .filter(e -> e.getValue().equals(rec))
                .findAny()
                .ifPresent(e -> startedOrders.remove(e.getKey()));
    }

    private void start(JobExecutionContext jobExecutionContext) {
        String name = jobExecutionContext.getJobDetail().getKey().getName();
        var order = recordingOrders.get(name);
        start(order);
    }

    private void start(ScheduledOrder scheduledOrder) {
        var order = scheduledOrder.order();
        String filename = createFilename(order.getChannel(), order.getStart());
        order.setFilename(filename);
        Recorder recorder = createRecorder();
        recorder.start(filename, order.getChannel().getName(), order.getChannel().getId());

        logger.info("Commanded recording until {}", formattedAtLocal(order.getEnd()));

        String id = order.getId();
        JobAndTrigger stopJob = createJobAndTrigger("stopRecordingJob" + id, "stopRecordingTrigger" + id, StopRecordingJob.class, getDate(order.getEnd()));
        schedule(stopJob);

        startedOrders.put("stopRecordingJob" + id, new Recording(order, stopJob, recorder));
        recordingOrders.entrySet().stream()
                .filter(e -> e.getValue().order().equals(order))
                .findAny()
                .ifPresent(e -> recordingOrders.remove(e.getKey()));
    }

    private JobDetail schedule(JobDetail job, Trigger trigger) {
        return SchedulingUtils.schedule(scheduler, job, trigger);
    }

    private JobDetail schedule(JobAndTrigger jobAndTrigger) {
        return schedule(jobAndTrigger.jobDetail(), jobAndTrigger.trigger());
    }

    private void cancelJob(JobAndTrigger jobAndTrigger) {
        SchedulingUtils.cancelJob(scheduler, jobAndTrigger);
    }

    private void cancelJob(ScheduledOrder order) {
        cancelJob(order.jobAndTrigger());
        recordingOrders.values().remove(order);
    }

    private boolean collides(RecordingOrderInternal recordingOrder) {
        var newRange = Range.closed(recordingOrder.getStart(), recordingOrder.getEnd());
        var scheduled = recordingOrders.values().stream()
                .map(ScheduledOrder::order);

        var current = startedOrders.values().stream()
                .map(Recording::order);

        return Stream.concat(scheduled, current)
                .filter(o -> !sameMultiplex(recordingOrder, o))
                .map(o -> Range.closed(o.getStart(), o.getEnd()))
                .anyMatch(r -> r.isConnected(newRange));
    }

    private boolean identical(RecordingOrderInternal recordingOrder) {
        return recordingOrders.values().stream().anyMatch(o -> o.order().equals(recordingOrder)) ||
                startedOrders.values().stream().anyMatch(o -> o.order().equals(recordingOrder));
    }

    private boolean sameMultiplex(RecordingOrderInternal first, RecordingOrderInternal second) {
        return second.getChannel().getMultiplexID().equals(first.getChannel().getMultiplexID());
    }

    private String createFilename(Channel channel, ZonedDateTime start) {
        return channel.getName() + " " + formattedAtLocalForFilename(start) + ".mp4";
    }

    private Recorder createRecorder() {
        return applicationContext.getBean(Recorder.class);
    }

    private record Recording(RecordingOrderInternal order, JobAndTrigger stop, Recorder recorder) {
    }

    private record ScheduledOrder(RecordingOrderInternal order, JobAndTrigger jobAndTrigger) {

        private ScheduledOrder(RecordingOrderInternal order, JobDetail jobDetail, Trigger trigger) {
            this(order, new JobAndTrigger(jobDetail, trigger));
        }

        public ScheduledOrder(RecordingOrderInternal order, JobAndTrigger jobAndTrigger) {
            this.order = order;
            this.jobAndTrigger = jobAndTrigger;
        }
    }

    public class StartRecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            start(jobExecutionContext);
        }
    }

    public class StopRecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            stop(jobExecutionContext);
        }
    }
}
