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

    //those are very temporary until channels are implemented
    String url = "http://raspberrypi:9981/stream/channel/6c34f91f6b869329a19e35ef03c9d47a";
    String filename = "./aa.mp4";

    @Autowired
    RecorderManager manager;
    @Value("${recording.defaultRecordingTimeInMinutes}")
    int defaultRecordingTime;

    @GetMapping("/record")
    public ResponseEntity<Void> record() {
        var start = LocalDateTime.now();
        var end = start.plus(Duration.ofMinutes(defaultRecordingTime));
        var recordingOrder = new RecordingOrder(url, filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/record/seconds/{time}")
    public ResponseEntity<Void> record1(@PathVariable("time") int time) {
        var start = LocalDateTime.now();
        var end = start.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrder(url, filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/record/after/{after}/time/{time}")
    public ResponseEntity<Void> record2(@PathVariable("time") int time, @PathVariable("after") int after) {
        var start = LocalDateTime.now().plus(Duration.ofSeconds(after));
        var end = start.plus(Duration.ofSeconds(time));
        var recordingOrder = new RecordingOrder(url, filename, start, end);

        manager.record(recordingOrder);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/stop")
    public ResponseEntity<Void> stop() {
        manager.stop();

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
