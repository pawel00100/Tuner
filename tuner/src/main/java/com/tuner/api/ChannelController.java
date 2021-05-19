package com.tuner.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.model.Channel;
import com.tuner.model.TVHChannelList;
import com.tuner.utils.rest_client.Requests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

@RestController
@RequestMapping("/api/channel")
public class ChannelController {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${tvheadened.url}")
    private String url;

    @GetMapping("/list")
    public ResponseEntity<List<Channel>> ok() throws IOException, InterruptedException {

        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/channel/list", authHeader);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        var read = mapper.readValue(response.body(), TVHChannelList.class);

        return new ResponseEntity<>(read.getEntries(), HttpStatus.OK);

    }


}

