package com.tuner.model.server_requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HeatbeatRequest {

    @JsonProperty("free_space")
    long freeSpace;

    public HeatbeatRequest(long freeSpace) {
        this.freeSpace = freeSpace;
    }
}
