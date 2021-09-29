package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.recorded_files.RecordListProvider;
import com.tuner.settings.SettingsProvider;
import com.tuner.utils.scheduling.SchedulingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
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


//This is temporary until heartbeat is implemented on server
@Service
@Slf4j
public class RecordListSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${polling.intervalInSeconds}")
    int interval = 30;
    @Autowired
    Scheduler scheduler;

    @Autowired
    RecordListProvider recordListProvider;
    @Value("${tuner.id}")
    String id;
    @Value("${tvheadened.url}")
    String tvhBaseURL;
    @Value("${server.url}")
    String serverURL;
    @Autowired
    SettingsProvider settingsProvider;

    @PostConstruct
    void postConstruct() throws SchedulerException {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "RecordListSenderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("RecordListSenderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

        settingsProvider.subscribe("tvheadened.url", c -> tvhBaseURL = c);
        settingsProvider.subscribe("server.url", c -> serverURL = c);
    }

    private void postRecordList() {
        String requestBody = null;
        try {
            var aa = recordListProvider.getRecordings();
            if (aa.isEmpty()) { //TODO: rethonk expretions
                log.debug("empty recorded file list provided");
                return;
            }
            requestBody = mapper.writeValueAsString(aa);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping recorded file list for sending to server", e);
        }

        URI uri = null;
        try {
            uri = new URIBuilder(serverURL + "/recorded")
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
            log.error("Failed posting recorded file list", e);
            return;
        }

        if (response.statusCode() == HttpStatus.SC_OK) {
            log.debug("Successfully sent recorded file list to server");
        } else {
            log.error("Failed posting recorded file list, got status code: " + response.statusCode() + " response body: " + response.body());
        }
    }


    private class HeartbeatJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try {
                postRecordList();
            } catch (Exception ex) {
                log.warn("failed to post recorded file list", ex);
            }
        }
    }
}
