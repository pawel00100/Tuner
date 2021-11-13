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
import com.tuner.utils.rest_client.RequestException;
import com.tuner.utils.rest_client.URLBuilder;
import com.tuner.utils.scheduling.SchedulingUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;


@Service
@Slf4j
public class RecordingOrdersFetcher {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${polling.intervalInSeconds.fast}")
    int interval;
    @Autowired
    Scheduler scheduler;

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
    @Autowired
    SettingsProvider settingsProvider;

    @PostConstruct
    void postConstruct() throws SchedulerException {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "recordingOrderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("recordingOrderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

        settingsProvider.subscribe("tvheadened.url", c -> tvhBaseURL = c);
        settingsProvider.subscribe("server.url", c -> serverURL = c);
    }

    private void getOrders() {
        channelProvider.getChannelList(); //TODO: temp for filling cache

        List<RecordingOrderExternal> ordersFromServer = null;
        try {
            ordersFromServer = new URLBuilder(serverURL + "/orders")
                    .setParameter("id", id)
                    .build()
                    .basicAuth("admin", "admin")
                    .GET()
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK()
                    .deserialize(new TypeReference<>() {
                    });
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping record orders received from server", e);
        } catch (RequestException e) {
            log.error("Failed fetching record orders", e);
        }

        log.debug("fetched " + ordersFromServer.size() + " recording orders from server");

        var orders = ordersFromServer.stream()
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
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Failed to find corresponding event in EPG"))
                .getTitle();  //TODO: rethink if it should be assigned here - maybe in request?
        return new RecordingOrderInternal(channelProvider.getChannel(o.getChannelID()), programName, startTime, endTime, o.getId(), true);
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
