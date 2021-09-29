package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import com.tuner.settings.SettingsProvider;
import com.tuner.utils.scheduling.SchedulingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;


@Service
@Slf4j
public class SettingsFetcher {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> changeableSettings = List.of();

    @Value("${polling.intervalInSeconds}")
    int interval = 10;
    @Autowired
    Scheduler scheduler;

    @Value("${tuner.id}")
    String id;

    @Value("${server.url}")
    String serverURL;
    @Autowired
    SettingsProvider settingsProvider;

    @PostConstruct
    void postConstruct() throws SchedulerException {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "settingsFetchTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("settingsFetchJob", SettingsFetchJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

        settingsProvider.subscribe("server.url", c -> serverURL = c);
    }

    private void getSettings() {
        URI uri = null;
        try {
            uri = new URIBuilder(serverURL + "/settings")
                    .setParameter("id", id)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
        String body = response.body();

        //TODO: probably mapping to Settings object, and having Setting as an enum with keys would be more elegant
        JsonNode tree = null;
        try {
            tree = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        Streams.stream(tree.get(0).fields())
                .filter(n -> changeableSettings.contains(n.getKey()))
                .forEach(n -> settingsProvider.set(n.getKey(), n.getValue().asText()));

    }


    private class SettingsFetchJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try {
                getSettings();
            } catch (Exception ex) {
                log.warn("failed to retrieve Settings", ex);
            }
        }
    }
}
