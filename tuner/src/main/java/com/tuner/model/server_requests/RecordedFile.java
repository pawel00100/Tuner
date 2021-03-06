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

    @JsonProperty("file_name")
    private String filename;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("channel_id")
    private String channelId;
    @JsonProperty("program_name")
    private String programName;
    private long start;
    @Column(name = "end_time")
    @JsonProperty("stop")
    private long end;
    private long length;
    @JsonProperty("record_size")
    private long size;
    @Transient
    private Channel channel;


    public RecordedFile(RecordingOrderInternal order, long end, long length, long size) {
        this.filename = order.getFilename();
        this.channelId = order.getChannel().getId();
        this.channel = order.getChannel();
        this.programName = order.getProgramName();
        this.start = order.getStart().toEpochSecond();
        this.end = end;
        this.orderId = order.getId();
        this.length = length;
        this.size = size;
    }

    public RecordedFile() {

    }
}
