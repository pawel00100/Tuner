package com.tuner.model.tvh_responses.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TVHService {
    String id;
    String multiplex;
    String multiplexID;

    String channelID;

    @JsonSetter("uuid")
    public void setId(String id) {
        this.id = id;
    }

    @JsonSetter("multiplex")
    public void setMultiplex(String multiplex) {
        this.multiplex = multiplex;
    }

    @JsonSetter("multiplex_uuid")
    public void setMultiplexID(String multiplexID) {
        this.multiplexID = multiplexID;
    }

    @JsonSetter("channel")
    public void setChannelIDs(List<String> channelIDs) {
        this.channelID = channelIDs.get(0);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TVHServiceList {
        List<TVHService> entries;

        public TVHServiceList(List<TVHService> entries) {
            this.entries = entries;
        }

        public TVHServiceList() {
        }
    }
}
