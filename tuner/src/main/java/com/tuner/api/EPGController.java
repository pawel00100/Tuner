package com.tuner.api;

import com.tuner.connector_to_tvh.EPGProvider;
import com.tuner.model.tvh_responses.EPGEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/epg")
public class EPGController {

    @Autowired
    EPGProvider epgProvider;

    @GetMapping("/list")
    public ResponseEntity<List<EPGEvent>> parsed() {

        List<EPGEvent> read = epgProvider.getParsed();

        return new ResponseEntity<>(read, HttpStatus.OK);
    }

    @GetMapping("/listRaw")  // unparsed, not recommended for use, but left for performance investigations
    public ResponseEntity<String> raw() {
        String body = epgProvider.getRaw();

        return new ResponseEntity<>(body, HttpStatus.OK);
    }


}

