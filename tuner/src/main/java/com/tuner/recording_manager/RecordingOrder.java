package com.tuner.recording_manager;

import java.time.LocalDateTime;

public class RecordingOrder {
    String id;
    String url;
    String filename;
    //TODO: analyze if ZonedTImeDate would provide better timezone safety
    LocalDateTime start;
    LocalDateTime finish;

    private static int num = 0;

    public RecordingOrder(String url, String filename, LocalDateTime start, LocalDateTime finish) {
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

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getFinish() {
        return finish;
    }

    public void setFinish(LocalDateTime finish) {
        this.finish = finish;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
