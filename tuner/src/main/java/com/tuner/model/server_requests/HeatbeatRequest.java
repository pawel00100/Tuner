package com.tuner.model.server_requests;

import lombok.Data;

@Data
public class HeatbeatRequest {

    String id;
    long freeSpace;
    boolean isRecording;
    int currentRecordingTime;
    int currentRecordingSize;

    public HeatbeatRequest(String id, long freeSpace, boolean isRecording, int currentRecordingTime, int currentRecordingSize) {
        this.id = id;
        this.freeSpace = freeSpace;
        this.isRecording = isRecording;
        this.currentRecordingTime = currentRecordingTime;
        this.currentRecordingSize = currentRecordingSize;
    }
}
