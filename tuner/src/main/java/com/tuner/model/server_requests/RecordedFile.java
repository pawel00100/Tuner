package com.tuner.model.server_requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuner.recording_manager.RecordingOrderInternal;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class RecordedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private int id;

    @JsonIgnore
    private String filename;

    private String channelId;
    @JsonProperty("program_name")
    private String programName;
    private long start;
    @Column(name = "end_time")
    private long end;
    private long length;
    @JsonProperty("record_size")
    private long size;

    public RecordedFile(String filename, String channelId, String programName, long start, long end, long length, long size) {
        this.filename = filename;
        this.channelId = channelId;
        this.programName = programName;
        this.start = start;
        this.end = end;
        this.length = length;
        this.size = size;
    }

    public RecordedFile(RecordingOrderInternal order, long length, long size) {
        this.filename = order.getFilename();
        this.channelId = order.getChannel().getId();
        this.programName = order.getProgramName();
        this.start = order.getStart().toEpochSecond();
        this.end = order.getEnd().toEpochSecond();
        this.length = length;
        this.size = size;
    }

    public RecordedFile() {

    }
}
