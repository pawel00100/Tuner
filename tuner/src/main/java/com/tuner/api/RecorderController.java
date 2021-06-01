package com.tuner.api;

import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.recording_manager.RecorderManager;
import com.tuner.recording_manager.RecordingOrderInternal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;

import static com.tuner.utils.TimezoneUtils.formattedAtLocalForFilename;

@RestController
@RequestMapping("/api/recorder")
public class RecorderController {

    @Autowired
    ChannelProvider channelProvider;
    @Autowired
    RecorderManager manager;
    @Value("${recording.defaultRecordingTimeInMinutes}")
    int defaultRecordingTime;
    @Value("${tvheadened.url}")
    String tvhBaseURL;

    @PostMapping("/record/channel/{channel}")
    public ResponseEntity<Void> record(@PathVariable("channel") String channel) {
        var startTime = ZonedDateTime.now(ZoneId.of("Z"));
        var endTime = startTime.plus(Duration.ofMinutes(defaultRecordingTime));
        var recordingOrder = new RecordingOrderInternal(fullURL(channel), createFilename(channel, startTime), startTime, endTime);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/record/channel/{channel}/seconds/{time}")
    public ResponseEntity<Void> record1(@PathVariable("channel") String channel, @PathVariable("time") int time) {
        var startTime = ZonedDateTime.now(ZoneId.of("Z"));
        var endTime = startTime.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrderInternal(fullURL(channel), createFilename(channel, startTime), startTime, endTime);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/record/channel/{channel}/after/{after}/time/{time}")
    public ResponseEntity<Void> record2(@PathVariable("channel") String channel, @PathVariable("time") int time, @PathVariable("after") int after) {
        var startTime = ZonedDateTime.now(ZoneId.of("Z")).plus(Duration.ofSeconds(after));
        var endTime = startTime.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrderInternal(fullURL(channel), createFilename(channel, startTime), startTime, endTime);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/record/channel/{channel}/start/{start}/end/{end}")
    public ResponseEntity<Void> recordTime(@PathVariable("channel") String channel, @PathVariable("start") int start, @PathVariable("end") int end) {
        var startTime = LocalDateTime.ofEpochSecond(start, 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        var endTime = LocalDateTime.ofEpochSecond(end, 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        var recordingOrder = new RecordingOrderInternal(fullURL(channel), createFilename(channel, startTime), startTime, endTime);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stop() {
        manager.stop();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String fullURL(String channel) {
        return tvhBaseURL + "/stream/channel/" + channel;
    }

    private String createFilename(String channel, ZonedDateTime start) {
        return channelProvider.getName(channel) + " " + formattedAtLocalForFilename(start) + ".mp4";
    }
}
