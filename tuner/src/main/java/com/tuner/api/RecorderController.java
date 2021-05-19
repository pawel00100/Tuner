package com.tuner.api;

import com.tuner.recording_manager.RecorderManager;
import com.tuner.recording_manager.RecordingOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;

@RestController
@RequestMapping("/api/recorder")
public class RecorderController {

    String filename = "./aa.mp4";

    @Autowired
    RecorderManager manager;
    @Value("${recording.defaultRecordingTimeInMinutes}")
    int defaultRecordingTime;
    @Value("${tvheadened.url}")
    String tvhBaseURL;

    @PostMapping("/record/channel/{channel}")
    public ResponseEntity<Void> record(@PathVariable("channel") String channel) {
        var start = ZonedDateTime.now(ZoneId.of("Z"));
        var end = start.plus(Duration.ofMinutes(defaultRecordingTime));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/record/channel/{channel}/seconds/{time}")
    public ResponseEntity<Void> record1(@PathVariable("channel") String channel, @PathVariable("time") int time) {
        var start = ZonedDateTime.now(ZoneId.of("Z"));
        var end = start.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/record/channel/{channel}/after/{after}/time/{time}")
    public ResponseEntity<Void> record2(@PathVariable("channel") String channel, @PathVariable("time") int time, @PathVariable("after") int after) {
        var start = ZonedDateTime.now(ZoneId.of("Z")).plus(Duration.ofSeconds(after));
        var end = start.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/record/channel/{channel}/start/{start}/end/{end}")
    public ResponseEntity<Void> recordTime(@PathVariable("channel") String channel, @PathVariable("start") int start, @PathVariable("end") int end) {
        var startTime = LocalDateTime.ofEpochSecond(start, 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        var endTime = LocalDateTime.ofEpochSecond(end, 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, startTime, endTime);

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
}
