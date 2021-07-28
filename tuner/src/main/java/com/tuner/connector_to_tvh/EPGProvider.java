package com.tuner.connector_to_tvh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tuner.model.tvh_responses.EPGEvent;
import com.tuner.model.tvh_responses.EPGObject;
import com.tuner.utils.rest_client.Requests;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EPGProvider {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Cache<String, List<EPGEvent>> epgCache = createCache();

    @Value("${tvheadened.url}")
    private String url;

    public List<EPGEvent> getParsed() {
        var retrieevd = epgCache.getIfPresent("A");
        if (retrieevd != null) {
            return retrieevd;
        }
        retrieevd = tryGettingParsed();
        if (!retrieevd.isEmpty()) {
            epgCache.put("A", retrieevd);
        }
        return retrieevd;
    }

    private List<EPGEvent> tryGettingParsed() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/epg/events/grid?limit=50000", authHeader);

        HttpResponse<String> response = null;
        List<EPGEvent> epg = Collections.emptyList();
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed getting epg from TVH");
            return epg;
        }

        if (response.statusCode() == HttpStatus.SC_OK) {
            log.debug("Successfully got epg from TVH");
        } else {
            log.error("Failed getting epg from TVH, got status code: " + response.statusCode() + " response body: " + response.body());
            return epg;
        }

        try {
            epg = mapper.readValue(response.body(), EPGObject.class).getEntries();
        } catch (JsonProcessingException e) {
            log.error("Failed mapping epg from TVH", e);
        }
        return epg;
    }

    public String getRaw() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/epg/events/grid?limit=50000", authHeader);

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed getting epg from TVH", e);
        }

        return response.body();
    }


    private <K, V> Cache<K, V> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
}
