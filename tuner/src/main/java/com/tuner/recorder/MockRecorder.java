package com.tuner.recorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
@ConditionalOnProperty(name = "recorder.mocked", havingValue = "true")
public class MockRecorder implements Recorder {
    private static final Logger logger = LoggerFactory.getLogger(MockRecorder.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Value("${recording.fileSizeLoggingIntervalInSeconds}")
    private static final int interval = 10;
    boolean recording = false;
    long startTime;
    private ScheduledFuture<?> scheduledFuture = null;

    private String channelName;

    public void start(String filename, String channelName, String channelId) {
        this.channelName = channelName;

        if (recording) {
            logger.error("Trying to record while recording is marked as true");
            return;
        }
        recording = true;
        startTime = System.currentTimeMillis();
        logger.info("Started mock recording channel " + channelName);
        scheduledFuture = executor.scheduleAtFixedRate(this::logFileSize, interval, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        logger.info("Stopped mock recording channel " + channelName);
        scheduledFuture.cancel(false);
    }

    public int getSize() {
        return recordingTimeInSeconds() * 500000;
    }

    public boolean isRecording() {
        return recording;
    }

    public int recordingTimeInSeconds() {
        if (!recording) {
            return 0;
        }

        long millis = System.currentTimeMillis() - startTime;
        return (int) (millis / 1000);
    }

    private void logFileSize() {
        logger.info("Mock recording channel {}. File size: {} MB", channelName, getSize() / 1000000);
    }

}
