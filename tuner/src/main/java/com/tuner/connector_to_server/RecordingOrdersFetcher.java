package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.connector_to_tvh.ChannelProvider;
import com.tuner.model.server_responses.RecordingOrderExternal;
import com.tuner.recording_manager.RecorderManager;
import com.tuner.recording_manager.RecordingOrderInternal;
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
import java.time.*;
import java.util.List;

import static com.tuner.utils.TimezoneUtils.formattedAtLocalForFilename;


@Service
@Slf4j
public class RecordingOrdersFetcher {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    int interval = 20;
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


    public RecordingOrdersFetcher(@Autowired Scheduler scheduler) throws SchedulerException {
        this.scheduler = scheduler;
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "recordingOrderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("recordingOrderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

    }

    private void getOrders() {
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

        obj.stream()
                .filter(o -> o.getStart() > System.currentTimeMillis() / 1000)
                .map(this::getRecordingOrder)
                .peek(o -> System.out.println(o.getStart()))
                .forEach(o -> manager.record(o));
    }

    private RecordingOrderInternal getRecordingOrder(RecordingOrderExternal o) {
        var startTime = LocalDateTime.ofEpochSecond(o.getStart(), 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        var endTime = LocalDateTime.ofEpochSecond(o.getEnd(), 0, ZoneOffset.UTC).atZone(ZoneId.of("Z"));
        return new RecordingOrderInternal(fullURL(o.getChannelID()), createFilename(o.getChannelID(), startTime), startTime, endTime);
    }

    private String fullURL(String channel) {
        return tvhBaseURL + "/stream/channel/" + channel;
    }

    private String createFilename(String channel, ZonedDateTime start) {
        return channelProvider.getName(channel) + " " + formattedAtLocalForFilename(start) + ".mp4";
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
