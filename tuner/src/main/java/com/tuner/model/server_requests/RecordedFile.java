package com.tuner.model.server_requests;

import lombok.Data;

@Data
public class RecordedFile {

    String channel;
    String programName;
    long start;
    long end;
    long length;
    long size;

}
