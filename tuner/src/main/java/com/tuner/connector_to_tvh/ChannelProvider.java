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
import com.tuner.utils.rest_client.RequestException;
import com.tuner.utils.rest_client.URLBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
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
        try {
            return new URLBuilder(url + "/api/mpegts/service/grid")
                    .build()
                    .basicAuth("aa", "aa")
                    .GET()
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK()
                    .deserialize(TVHService.TVHServiceList.class)
                    .getEntries();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping services received from server", e);
        } catch (RequestException e) {
            log.error("Failed fetching services", e);
        }
        return Collections.emptyList();
    }

    private List<TVHChannel> tryGettingTVHChannels() {
        try {
            return new URLBuilder(url + "/api/channel/list")
                    .build()
                    .basicAuth("aa", "aa")
                    .GET()
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK()
                    .deserialize(TVHChannel.TVHChannelList.class)
                    .getEntries();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping channel list received from server", e);
        } catch (RequestException e) {
            log.error("Failed fetching channel list", e);
        }
        return Collections.emptyList();
    }

    //TODO: replace with malual map use with 15 min timer  make as singleton
    private <K, V> Cache<K, V> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
}
