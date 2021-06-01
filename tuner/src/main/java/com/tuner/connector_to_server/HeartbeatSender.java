package com.tuner.connector_to_server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuner.model.server_requests.HeatbeatRequest;
import com.tuner.model.server_responses.HeartbeatResponse;
import com.tuner.recorder.StreamRecorder;
import com.tuner.utils.SchedulingUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


//@Service
@Slf4j
public class HeartbeatSender {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    Scheduler scheduler;
    @Autowired
    StreamRecorder recorder;
    @Value("${tuner.id}")
    String id;
    //    @Value("${heartbeat.intervalInSeconds}")
    int interval = 1;

    public HeartbeatSender(@Autowired
                                   Scheduler scheduler) throws SchedulerException {
        this.scheduler = scheduler;
        Trigger trigger = SchedulingUtils.getScheduledTrigger(Duration.ofSeconds(interval), "heartbeatTrigger");
        JobDetail jobDetail = SchedulingUtils.getJobDetail("heartbeatJob", HeartbeatJob.class);

        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void heartbeat() throws IOException, InterruptedException {
        var status = new HeatbeatRequest(id, 0, recorder.isRecording(), recorder.recordingTimeInSeconds(), recorder.getSize());
        String requestBody = mapper.writeValueAsString(status);

        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://146ae477-c3bf-4841-87d2-bf550ac29bf5.mock.pstmn.io"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .header("x-api-key", "PMAK-60a837fa20c3cd002adf8528-4c16dcca253cf7d10cdf9861101fb2bb2d")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        var obj = mapper.readValue(body, HeartbeatResponse.class);

        System.out.println(obj.isNeedEPG());
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
