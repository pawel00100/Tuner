package com.tuner.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tuner.model.EPGEvent;
import com.tuner.model.EPGObject;
import com.tuner.utils.rest_client.Requests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/epg")
public class EPGController {

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final LoadingCache<HttpRequest, HttpResponse<String>> epgCache =
            createEPGCache();


    @Value("${tvheadened.url}")
    private String url;

    @GetMapping("/list")
    public ResponseEntity<List<EPGEvent>> parsed() throws IOException, ExecutionException {

        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/epg/events/grid?limit=50000", authHeader);

        HttpResponse<String> response = epgCache.get(request);

        var read = mapper.readValue(response.body(), EPGObject.class);


        return new ResponseEntity<>(read.getEntries(), HttpStatus.OK);

    }

    @GetMapping("/listRaw")  // unparsed, not recommended for use, but left for performance investigations
    public ResponseEntity<String> raw() throws IOException, InterruptedException {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/epg/events/grid?limit=50000", authHeader);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    private LoadingCache<HttpRequest, HttpResponse<String>> createEPGCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public HttpResponse<String> load(HttpRequest request) throws Exception {
                        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    }
                });
    }

}

