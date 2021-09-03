package com.tuner.recorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "recorder.mocked", havingValue = "true")
public class MockRecorder implements Recorder {
    private static final Logger logger = LoggerFactory.getLogger(MockRecorder.class);

    boolean recording = false;
    long startTime;


    public void start(String filename, String url) {
        if (recording) {
            logger.error("Trying to record while recording is marked as true");
            return;
        }
        recording = true;
        startTime = System.currentTimeMillis();
        logger.debug("started mock recording");
    }

    public void stop() {
        logger.info("stopped mock recording");
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


}
