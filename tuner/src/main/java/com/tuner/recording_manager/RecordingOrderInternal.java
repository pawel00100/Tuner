package com.tuner.recording_manager;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Objects;

//TODO: reconsider not using this class, but keep in original format from server, so setting filename and url is at the later stage

@Data
public class RecordingOrderInternal {
    private static int num = 0;
    private final String id;
    private final String channelId;
    private final String programName;
    private final ZonedDateTime plannedStart; //for recognition of same origin on server TODO: replace with id form server
    private final boolean fromServer; //if true order should be deleted if not on server
    private String filename;
    private ZonedDateTime start;
    private ZonedDateTime end;

    public RecordingOrderInternal(String channelId, String programName, ZonedDateTime start, ZonedDateTime end, boolean fromServer) {
        this.channelId = channelId;
        this.programName = programName;
        this.start = start;
        this.plannedStart = start;
        this.end = end;
        num++;
        this.id = Integer.toString(num);
        this.fromServer = fromServer;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RecordingOrderInternal)) {
            return false;
        }
        var other = (RecordingOrderInternal) obj;
        return Objects.equals(this.channelId, other.channelId) &&
                Objects.equals(this.plannedStart, other.plannedStart) &&
                Objects.equals(this.end, other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, plannedStart, end);
    }
}
