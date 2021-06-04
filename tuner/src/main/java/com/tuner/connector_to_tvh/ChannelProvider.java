package com.tuner.connector_to_tvh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tuner.model.tvh_responses.Channel;
import com.tuner.model.tvh_responses.TVHChannelList;
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
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChannelProvider {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Cache<String, List<Channel>> channelCache = createCache();


    @Value("${tvheadened.url}")
    private String url;

    //TODO: cache results - will require making as singleton
    public List<Channel> getChannelList() {
        var retrieevd = channelCache.getIfPresent("A");
        if (retrieevd != null) {
            return retrieevd;
        }
        retrieevd = tryGettingChannels();
        if (!retrieevd.isEmpty()) {
            channelCache.put("A", retrieevd);
        }
        return retrieevd;
    }

    private List<Channel> tryGettingChannels() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/channel/list", authHeader);

        HttpResponse<String> response = null;
        List<Channel> channels = Collections.emptyList();
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed getting channel list from TVH", e);
            return channels;
        }

        if (response.statusCode() == HttpStatus.SC_OK) {
            log.debug("Successfully got channel list from TVH");
        } else {
            log.error("Failed posting channel list, got status code: " + response.statusCode() + " response body: " + response.body());
            return channels;
        }

        try {
            channels = mapper.readValue(response.body(), TVHChannelList.class).getEntries();
        } catch (JsonProcessingException e) {
            log.error("Failed mapping channel list from TVH", e);
            e.printStackTrace();
        }
        return channels;
    }

    //TODO: have id to name map for performance reasons
    public String getName(String channel) {
        return getChannelList().stream().filter(c -> c.getId().equals(channel)).findFirst().orElseThrow(() -> new NoSuchElementException("Channel id not found " + channel)).getName();
    }

    //TODO: replace with malual map use with 15 min timer  make as singleton
    private <K, V> Cache<K, V> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
}
