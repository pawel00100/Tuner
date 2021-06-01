package com.tuner.model.server_responses;

import lombok.Data;

@Data
public class HeartbeatResponse {

    boolean needEPG;
    boolean needRecordingFileList;
    boolean changedRecordingOrderList;
    boolean changedSettings;
}
