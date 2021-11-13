package com.tuner.model.server_responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordingOrderExternal {

    @JsonProperty("order_id")
    String id;
    @JsonProperty("channel_uuid")
    String channelID;
    long start;
    @JsonProperty("stop")
    long end;


}
