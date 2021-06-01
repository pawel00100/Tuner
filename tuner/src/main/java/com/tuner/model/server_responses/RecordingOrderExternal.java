package com.tuner.model.server_responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecordingOrderExternal {

    @JsonProperty("channel_id")
    String channelID;
    long start;
    long end;


}
