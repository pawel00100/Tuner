package com.tuner.connector_to_tvh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tuner.model.server_requests.Channel;
import com.tuner.model.tvh_responses.channel.TVHChannel;
import com.tuner.model.tvh_responses.channel.TVHService;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    public ChannelProvider(SettingsProvider settingsProvider) {
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

    //TODO: have id to name map for performance reasons, harden for NPE
    public Channel getChannel(String channelId) {
        return getChannelList().stream().filter(c -> c.getId().equals(channelId)).findFirst().orElseThrow(() -> new NoSuchElementException("Channel id not found " + channelId));
    }

    public String getName(String channelId) {
        return getChannel(channelId).getName();
    }

    private List<Channel> tryGettingChannels() {
        var channels = tryGettingTVHChannels().stream().collect(Collectors.toMap(TVHChannel::getId, s -> s));
        var services = tryGettingTVHServices();

        return services.stream()
                .map(s -> getChannel(s, channels))
                .toList();
    }

    private Channel getChannel(TVHService service, Map<String, TVHChannel> channelMap) {
        TVHChannel channel = channelMap.get(service.getChannelID());
        return new Channel(channel.getId(), channel.getName(), service.getMultiplex(), service.getMultiplexID());
    }

    private List<TVHService> tryGettingTVHServices() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/mpegts/service/grid", authHeader);

        HttpResponse<String> response = null;
        List<TVHService> services = Collections.emptyList();

        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed getting channel list from TVH");
            return services;
        }

        if (response.statusCode() == HttpStatus.SC_OK) {
            log.debug("Successfully got channel list from TVH");
        } else {
            log.error("Failed posting channel list, got status code: " + response.statusCode() + " response body: " + response.body());
            return services;
        }

        try {
            System.out.println(response.body());
            services = mapper.readValue(response.body(), TVHService.TVHServiceList.class).getEntries();
        } catch (JsonProcessingException e) {
            log.error("Failed mapping channel list from TVH", e);
            e.printStackTrace();
        }
        return services;
    }

    private List<TVHChannel> tryGettingTVHChannels() {
        String authHeader = Requests.getAuthHeader("aa", "aa");
        var request = Requests.httpRequestWithAuth(url + "/api/channel/list", authHeader);

        HttpResponse<String> response = null;
        List<TVHChannel> channels = Collections.emptyList();

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
            channels = mapper.readValue(response.body(), TVHChannel.TVHChannelList.class).getEntries();
        } catch (JsonProcessingException e) {
            log.error("Failed mapping channel list from TVH", e);
            e.printStackTrace();
        }
        return channels;
    }

    //TODO: replace with malual map use with 15 min timer  make as singleton
    private <K, V> Cache<K, V> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
}
