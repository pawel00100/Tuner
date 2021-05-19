package com.tuner.recording_manager;

import java.time.ZonedDateTime;

public class RecordingOrder {
    String id;
    String url;
    String filename;
    ZonedDateTime start;
    ZonedDateTime finish;

    private static int num = 0;

    public RecordingOrder(String url, String filename, ZonedDateTime start, ZonedDateTime finish) {
        this.url = url;
        this.filename = filename;
        this.start = start;
        this.finish = finish;
        num++;
        this.id = Integer.toString(num);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public void setStart(ZonedDateTime start) {
        this.start = start;
    }

    public ZonedDateTime getFinish() {
        return finish;
    }

    public void setFinish(ZonedDateTime finish) {
        this.finish = finish;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
