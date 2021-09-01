package com.tuner.connector_to_tvh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tuner.model.tvh_responses.Channel;
import com.tuner.model.tvh_responses.TVHChannelList;
import com.tuner.persistence.db.ChannelListDAO;
import com.tuner.settings.SettingsProvider;
import com.tuner.utils.rest_client.Requests;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    ChannelListDAO dao;


    @Value("${tvheadened.url}")
    private String url;

    public ChannelProvider(@Autowired SettingsProvider settingsProvider) {
        settingsProvider.subscribe("tvheadened.url", c -> url = c);
    }


    //TODO: make as singleton?
    public List<Channel> getChannelList() {
        var retrieved = channelCache.getIfPresent("A");
        if (retrieved != null) {
            return retrieved;
        }
        retrieved = tryGettingChannels();
        if (!retrieved.isEmpty()) {
            channelCache.put("A", retrieved);
            dao.deleteAll();
            dao.addAll(retrieved);
            return retrieved;
        }
        return dao.getAll();
    }

    private List<Channel> tryGettingChannels() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/channel/list", authHeader);

        HttpResponse<String> response = null;
        List<Channel> channels = Collections.emptyList();
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed getting channel list from TVH");
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
