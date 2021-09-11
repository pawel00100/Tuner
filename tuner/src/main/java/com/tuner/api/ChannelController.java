package com.tuner.api;

import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.model.server_requests.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/channel")
public class ChannelController {
    @Autowired
    ChannelProvider channelProvider;

    @GetMapping("/list")
    public ResponseEntity<List<Channel>> ok() {

        return new ResponseEntity<>(channelProvider.getChannelList(), HttpStatus.OK);
    }

}

