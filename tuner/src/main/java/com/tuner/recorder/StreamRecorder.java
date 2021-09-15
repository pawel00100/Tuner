package com.tuner.recorder;

import com.tuner.settings.SettingsProvider;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
@ConditionalOnProperty(name = "recorder.mocked", havingValue = "false", matchIfMissing = true)
public class StreamRecorder implements Recorder {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(StreamRecorder.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    @Value("${recording.fileSizeLoggingIntervalInSeconds}")
    private static final int interval = 10;

    private final byte[] buffer = new byte[102400];
    @Value("${tvheadened.username}")
    String username;
    @Value("${tvheadened.password}")
    String password;
    @Value("${recording.location}")
    String location;
    @Value("${tvheadened.url}")
    String tvhBaseURL;
    private String filename;
    private String channelName;
    private String channelId;
    private boolean allowedRecording = false;
    private boolean recording = false;
    private int size = 0;
    private long startTime;
    private ScheduledFuture<?> scheduledFuture = null;

    public StreamRecorder(@Autowired SettingsProvider settingsProvider) {
        settingsProvider.subscribe("recording.location", c -> location = c);
    }

    @Override
    public void start(String filename, String channelName, String channelId) {
        this.filename = filename;
        this.channelName = channelName;
        this.channelId = channelId;
        recording = true;
        new Thread(() -> {
            try {
                recordInternal();
            } catch (IOException | InterruptedException e) { //TODO: Handle exceptions
                e.printStackTrace();
            }
        }).start();
        startTime = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        allowedRecording = false;
        scheduledFuture.cancel(false);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isRecording() {
        return recording;
    }

    @Override
    public int recordingTimeInSeconds() {
        if (!recording) {
            return 0;
        }

        long millis = System.currentTimeMillis() - startTime;
        return (int) (millis / 1000);
    }

    private void recordInternal() throws IOException, InterruptedException {
        InputStream stream = getStream(fullURL(channelId));
        allowedRecording = true;

        File file = new File(location, filename);
        if (file.exists()) {
            file.delete();
        }

        logger.info("Started recording channel " + channelName);
        scheduledFuture = executor.scheduleAtFixedRate(this::logFileSize, interval, interval, TimeUnit.SECONDS);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {

            int result = stream.read(buffer);

            while (result != -1 && allowedRecording) {
                size += result;
                outputStream.write(buffer, 0, result);
                result = stream.read(buffer);
            }
        }
        stream.close();
        recording = false;
        size = 0;
        logger.info("Stopped recording channel " + channelName);

    }

    private void logFileSize() {
        logger.info("Recording channel {}. File size: {} MB", channelName, getSize() / 1000000);
    }

    private String fullURL(String channelId) {
        return tvhBaseURL + "/stream/channel/" + channelId;
    }

    private InputStream getStream(String url) throws IOException, InterruptedException {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + new String(encodedAuth))
                .GET()
                .build(); //TODO: add checking for status code
        return client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
    }
}
