package com.tuner.api;

import com.tuner.recording_manager.RecorderManager;
import com.tuner.recording_manager.RecordingOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

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

    @GetMapping("/record/channel/{channel}")
    public ResponseEntity<Void> record(@PathVariable("channel") String channel) {
        var start = LocalDateTime.now();
        var end = start.plus(Duration.ofMinutes(defaultRecordingTime));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/record/channel/{channel}/seconds/{time}")
    public ResponseEntity<Void> record1(@PathVariable("channel") String channel, @PathVariable("time") int time) {
        var start = LocalDateTime.now();
        var end = start.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/record/channel/{channel}/after/{after}/time/{time}")
    public ResponseEntity<Void> record2(@PathVariable("channel") String channel, @PathVariable("time") int time, @PathVariable("after") int after) {
        var start = LocalDateTime.now().plus(Duration.ofSeconds(after));
        var end = start.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrder(fullURL(channel), filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/stop")
    public ResponseEntity<Void> stop() {
        manager.stop();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String fullURL(String channel) {
        return tvhBaseURL + "/stream/channel/" + channel;
    }
}
