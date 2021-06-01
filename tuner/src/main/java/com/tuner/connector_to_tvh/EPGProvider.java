package com.tuner.connector_to_tvh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tuner.model.tvh_responses.EPGEvent;
import com.tuner.model.tvh_responses.EPGObject;
import com.tuner.utils.rest_client.Requests;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EPGProvider {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final LoadingCache<HttpRequest, HttpResponse<String>> epgCache = createEPGCache();
    @Value("${tvheadened.url}")
    private String url;

    public List<EPGEvent> getParsed() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/epg/events/grid?limit=50000", authHeader);

        HttpResponse<String> response = null;
        List<EPGEvent> epg = Collections.emptyList();
        try {
            response = epgCache.get(request);
        } catch (ExecutionException e) {
            log.error("Failed getting epg from TVH", e);
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

    //TODO: don't save in cache if failure, right now will keep failure responses, make as singleton
    private LoadingCache<HttpRequest, HttpResponse<String>> createEPGCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public HttpResponse<String> load(HttpRequest request) throws Exception {
                        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    }
                });
    }
}
