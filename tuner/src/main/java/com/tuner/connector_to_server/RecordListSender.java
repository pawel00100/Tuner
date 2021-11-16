package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tuner.recorded_files.RecordListProvider;
import com.tuner.settings.SettingsProvider;
import com.tuner.utils.rest_client.RequestException;
import com.tuner.utils.rest_client.URLBuilder;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


//This is temporary until heartbeat is implemented on server
@Service
@Slf4j
public class RecordListSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${polling.intervalInSeconds}")
    int interval = 30;
    @Autowired
    Scheduler scheduler;
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    @Autowired
    RecordListProvider recordListProvider;
    @Value("${tuner.id}")
    String id;
    @Autowired
    SettingsProvider settingsProvider;

    @PostConstruct
    void postConstruct() {
        postRecordList();
        recordListProvider.subscribe(() -> executor.schedule(this::postRecordList, 2, TimeUnit.SECONDS));
    }

    public void postRecordList() {
        var recordings = recordListProvider.getRecordings().stream()
                .filter(r -> r.getEnd() <= (System.currentTimeMillis() / 1000))
                .toList();

        if (recordings.isEmpty()) {
            log.debug("empty recorded file list provided");
            return;
        }

        try {
            new URLBuilder(settingsProvider.getServerURL() + "/recorded")
                    .setParameter("id", id)
                    .build()
                    .auth(settingsProvider.getServerCredentials())
                    .post(recordings)
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping recorded file list for sending to server", e);
        } catch (RequestException e) {
            log.error("Failed posting recorded file list", e);
        }
        log.debug("posted recorded file list");
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
