package com.tuner.model.server_responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Settings {

    @JsonProperty("recording_location")
    String recordingLocation;
    @JsonProperty("tvh_username")
    String TVHUsername;
    @JsonProperty("tvh_password")
    String TVHpassword;

}
