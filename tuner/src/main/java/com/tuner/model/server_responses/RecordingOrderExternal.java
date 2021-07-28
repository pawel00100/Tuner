package com.tuner.model.server_responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordingOrderExternal {

    @JsonProperty("channel_id")
    String channelID;
    long start;
    @JsonProperty("end")
    long end;


}
