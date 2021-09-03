package com.tuner.recorder;

public interface Recorder {
    void start(String filename, String url);

    void stop();

    int getSize();

    boolean isRecording();

    int recordingTimeInSeconds();
}
