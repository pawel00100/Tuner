package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.connector_to_tvh.EPGProvider;
import com.tuner.model.server_responses.RecordingOrderExternal;
import com.tuner.recording_manager.RecorderManager;
import com.tuner.recording_manager.RecordingOrderInternal;
import com.tuner.settings.SettingsProvider;
import com.tuner.utils.SchedulingUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;


@Service
@Slf4j
public class RecordingOrdersFetcher {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final int interval = 10;
    private final Scheduler scheduler;

    @Autowired
    RecorderManager manager;
    @Autowired
    ChannelProvider channelProvider;
    @Autowired
    EPGProvider epgProvider;
    @Value("${tuner.id}")
    String id;
    @Value("${tvheadened.url}")
    String tvhBaseURL;
    @Value("${server.url}")
    String serverURL;


    public RecordingOrdersFetcher(@Autowired Scheduler scheduler, @Autowired SettingsProvider settingsProvider) throws SchedulerException {
        this.scheduler = scheduler;
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "recordingOrderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("recordingOrderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

        settingsProvider.subscribe("tvheadened.url", c -> tvhBaseURL = c);
        settingsProvider.subscribe("server.url", c -> serverURL = c);
    }

    private void getOrders() {
        channelProvider.getChannelList(); //TODO: temp for filling cache

        URI uri = null;
        try {
            uri = new URIBuilder(serverURL + "/orders")
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
        }
        String body = response.body();

        List<RecordingOrderExternal> obj = null;
        try {
            obj = mapper.readValue(body, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        log.debug("fetched " + obj.size() + " recording orders from server");

        var orders = obj.stream()
                .filter(o -> o.getEnd() > System.currentTimeMillis() / 1000)
                .map(this::getRecordingOrder).toList();

        manager.scheduleRecording(orders);
    }

    private RecordingOrderInternal getRecordingOrder(RecordingOrderExternal o) {
        var startTime = LocalDateTime.ofEpochSecond(o.getStart(), 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        var endTime = LocalDateTime.ofEpochSecond(o.getEnd(), 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));

        var programName = epgProvider.getParsed().stream()
                .filter(e -> e.getChannelUuid().equals(o.getChannelID()))
                .filter(e -> e.getStart() == o.getStart())
                .findAny().get().getTitle();  //TODO: rethink if it should be assigned here - maybe in request?
        return new RecordingOrderInternal(o.getChannelID(), programName, startTime, endTime, true);
    }

    private class HeartbeatJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try {
                getOrders();
            } catch (Exception ex) {
                log.warn("failed to retrieve Recording orders", ex);
            }
        }
    }
}
