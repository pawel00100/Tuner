package com.tuner.recording_manager;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Objects;

//TODO: reconsider not using this class, but keep in original format from server, so setting filename and url is at the later stage

@Data
public class RecordingOrderInternal {
    private static int num = 0;
    String id;
    String url;
    String filename;
    ZonedDateTime start;
    ZonedDateTime end;

    public RecordingOrderInternal(String url, String filename, ZonedDateTime start, ZonedDateTime end) {
        this.url = url;
        this.filename = filename;
        this.start = start;
        this.end = end;
        num++;
        this.id = Integer.toString(num);
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
        return Objects.equals(this.filename, other.filename) && Objects.equals(this.start, other.start) && Objects.equals(this.end, other.end);
    }
}
