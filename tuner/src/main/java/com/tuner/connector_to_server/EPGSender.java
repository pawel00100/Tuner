package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.connector_to_tvh.EPGProvider;
import com.tuner.recording_manager.RecorderManager;
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


//This is temporary until heartbeat is implemented on server
@Service
@Slf4j
public class EPGSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${polling.intervalInSeconds}")
    int interval = 30;
    @Autowired
    Scheduler scheduler;

    @Autowired
    RecorderManager manager;
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
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "EPGlSenderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("EPGSenderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);

        settingsProvider.subscribe("tvheadened.url", c -> tvhBaseURL = c);
        settingsProvider.subscribe("server.url", c -> serverURL = c);
    }

    private void postEPG() {
        var epg = epgProvider.getParsed();
        if (epg.isEmpty()) {
            log.debug("empty epg provided");
            return;
        }

        try {
            new URLBuilder(serverURL + "/epg")
                    .setParameter("id", id)
                    .build()
                    .post(epg)
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping epg for sending to server", e);
        } catch (RequestException e) {
            log.error("Failed posting epg", e);
        }
    }


    private class HeartbeatJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try {
                postEPG();
            } catch (Exception ex) {
                log.warn("failed to retrieve Recording orders", ex);
            }
        }
    }
}
