package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.recording_manager.RecorderManager;
import com.tuner.settings.SettingsProvider;
import com.tuner.utils.scheduling.SchedulingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


//This is temporary until heartbeat is implemented on server
@Service
@Slf4j
public class ChannelSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    int interval = 30;
    Scheduler scheduler;

    @Autowired
    RecorderManager manager;
    @Autowired
    ChannelProvider channelProvider;
    @Value("${tuner.id}")
    String id;
    @Value("${tvheadened.url}")
    String tvhBaseURL;
    @Value("${server.url}")
    String serverURL;


    public ChannelSender(@Autowired Scheduler scheduler, @Autowired SettingsProvider settingsProvider) throws SchedulerException {
        this.scheduler = scheduler;
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "ChannelSenderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("ChannelSenderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

        settingsProvider.subscribe("tvheadened.url", c -> tvhBaseURL = c);
        settingsProvider.subscribe("server.url", c -> serverURL = c);
    }

    private void postChannels() {
        String requestBody = null;
        try {
            var aa = channelProvider.getChannelList();
            if (aa.isEmpty()) { //TODO: temporary, rethunk expretions
                log.error("no channels returned");
                return;
            }
            requestBody = mapper.writeValueAsString(aa);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping channel list for sending to server", e);
            return;
        }

        URI uri = null;
        try {
            uri = new URIBuilder(serverURL + "/channels")
                    .setParameter("id", id)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        var request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Failed posting channel list", e);
            return;
        }

        if (response.statusCode() == HttpStatus.SC_OK) {
            log.debug("Successfully sent channel list to server");
        } else {
            log.error("Failed posting channel list, got status code: " + response.statusCode() + " response body: " + response.body());
        }
    }


    private class HeartbeatJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try { //technically no longer necessary, but a reasonable practice
                postChannels();
            } catch (Exception ex) {
                log.error("failed to retrieve Recording orders", ex);
            }
        }
    }
}
