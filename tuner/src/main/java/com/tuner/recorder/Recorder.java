package com.tuner.recorder;

public interface Recorder {
    void start(String filename, String channelName, String channelId);

    void stop();

    int getSize();

    boolean isRecording();

    int recordingTimeInSeconds();
}
