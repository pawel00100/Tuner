package com.tuner.connector_to_tvh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tuner.model.tvh_responses.EPGEvent;
import com.tuner.model.tvh_responses.EPGObject;
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
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EPGProvider {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Cache<String, List<EPGEvent>> epgCache = createCache();

    @Value("${tvheadened.url}")
    private String url;

    public EPGProvider(@Autowired SettingsProvider settingsProvider) {
        settingsProvider.subscribe("tvheadened.url", c -> url = c);
    }

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

    public String getRaw() {
        try {
            return new URLBuilder(url + "/api/epg/events/grid?limit=50000")
                    .build()
                    .basicAuth("aa", "aa")
                    .GET()
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK()
                    .getBody();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (RequestException e) {
            log.error("Failed fetching epg", e);
        }
        return null;
    }

    private List<EPGEvent> tryGettingParsed() {
        try {
            return new URLBuilder(url + "/api/epg/events/grid?limit=50000")
                    .build()
                    .basicAuth("aa", "aa")
                    .GET()
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK()
                    .deserialize(EPGObject.class)
                    .getEntries();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping epg received from server", e);
        } catch (RequestException e) {
            log.error("Failed fetching epg", e);
        }
        return Collections.emptyList();
    }


    private <K, V> Cache<K, V> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(5)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
}
