package com.tuner.model.tvh_responses;

import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Data
public class EPGEvent {
    int eventId;
    int episodeId;
    String channelName;
    String channelUuid;
    String channelNumber;
    String channelIcon;
    long start;
    long stop;
    String title;
    String subtitle;
    String summary;
    String description;
    List<Integer> genre;
    int nextEventId;

    @Override
    public String toString() {
        return String.format("%s [%s %s]", title, channelName, Instant.ofEpochSecond(start).atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

}
