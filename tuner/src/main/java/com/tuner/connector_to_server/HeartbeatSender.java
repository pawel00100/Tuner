package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tuner.model.server_requests.HeatbeatRequest;
import com.tuner.model.server_responses.HeartbeatResponse;
import com.tuner.recorder.Recorder;
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
import java.io.File;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Duration;


@Service
@Slf4j
public class HeartbeatSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final File currentDir = new File(".");
    private static final HeartbeatResponse readHeartbeat = new HeartbeatResponse(false, false, false, false);
    @Autowired
    Scheduler scheduler;
    @Autowired
    EPGSender epgSender;
    @Autowired
    RecordingOrdersFetcher ordersFetcher;
    @Autowired
    ChannelSender channelSender;
    @Autowired
    RecordListSender recordListSender;
    @Autowired
    Recorder recorder;
    @Value("${tuner.id}")
    String id;
    @Value("${heartbeat.intervalInSeconds}")
    int interval;
    @Autowired
    SettingsProvider settingsProvider;

    @PostConstruct
    void postConstruct() throws SchedulerException {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "heartbeatTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("heartbeatJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void heartbeat() {
        var status = new HeatbeatRequest(id, getFreeSpace(), recorder.isRecording(), recorder.recordingTimeInSeconds(), recorder.getSize());
        HeartbeatResponse obj = sendHeartbeat();
        if (obj.isNeedEPG()) {
            confirmReceived("post_epg");
            epgSender.postEPG();
        }
        if (obj.isChangedRecordingOrderList()) {
            confirmReceived("changed_recording_order_list");
            ordersFetcher.getOrders();
        }
        if (obj.isNeedRecordingFileList()) {
            confirmReceived("need_recording_file_list");
            recordListSender.postRecordList();
        }
    }

    private HeartbeatResponse sendHeartbeat() {
        HeartbeatResponse status = null;
        try {
            status = new URLBuilder(settingsProvider.getServerURL() + "/heartbeat")
                    .setParameter("id", id)
                    .build()
                    .auth(settingsProvider.getServerCredentials())
                    .GET()
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK()
                    .deserialize(new TypeReference<>() {
                    });
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping heartbeat received from server", e);
        } catch (RequestException e) {
            log.error("Failed fetching heartbeat", e);
        }

        return status;
    }

    private void confirmReceived(String updated) {
        try {
            new URLBuilder(settingsProvider.getServerURL() + "/heartbeat/provide")
                    .setParameter("id", id)
                    .setParameter("information", updated)
                    .build()
                    .auth(settingsProvider.getServerCredentials())
                    .post("")
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (RequestException e) {
            log.error("Failed posting heartbeat response", e);
        }
        log.debug("posted heartbeat response");
    }

    private long getFreeSpace() {
        return currentDir.getFreeSpace() / 1000000;
    }


    private class HeartbeatJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try {
                heartbeat();
            } catch (Exception ex) {
                log.warn("failed to publish heartbeat", ex);
            }
        }
    }
}
