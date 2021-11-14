package com.tuner.model.server_responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HeartbeatResponse {

    @JsonProperty("need_epg")
    boolean needEPG;
    @JsonProperty("need_recording_file_list")
    boolean needRecordingFileList;
    @JsonProperty("changed_recording_order_list")
    boolean changedRecordingOrderList;
    @JsonProperty("changed_settings")
    boolean changedSettings;

    public HeartbeatResponse() {
    }

    public HeartbeatResponse(boolean needEPG, boolean needRecordingFileList, boolean changedRecordingOrderList, boolean changedSettings) {
        this.needEPG = needEPG;
        this.needRecordingFileList = needRecordingFileList;
        this.changedRecordingOrderList = changedRecordingOrderList;
        this.changedSettings = changedSettings;
    }
}
