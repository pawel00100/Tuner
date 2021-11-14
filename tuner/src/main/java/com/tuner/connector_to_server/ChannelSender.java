package com.tuner.connector_to_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tuner.connector_to_tvh.ChannelProvider;
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
public class ChannelSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${polling.intervalInSeconds}")
    int interval = 30;
    @Autowired
    Scheduler scheduler;

    @Autowired
    RecorderManager manager;
    @Autowired
    ChannelProvider channelProvider;
    @Value("${tuner.id}")
    String id;
    @Autowired
    SettingsProvider settingsProvider;

    @PostConstruct
    void postConstruct() throws SchedulerException {
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "ChannelSenderTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("ChannelSenderJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);
    }

    void postChannels() {
        try {
            new URLBuilder(settingsProvider.getServerURL() + "/channels")
                    .setParameter("id", id)
                    .build()
                    .auth(settingsProvider.getServerCredentials())
                    .post(channelProvider.getChannelList())
                    .build(httpClient)
                    .send()
                    .assertStatusCodeOK();
        } catch (URISyntaxException e) {
            log.error("Failed building URI", e);
        } catch (JsonProcessingException e) {
            log.error("Failed mapping channel list for sending to server", e);
        } catch (RequestException e) {
            log.error("Failed posting channel list", e);
        }
        log.debug("posted channel list");
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
