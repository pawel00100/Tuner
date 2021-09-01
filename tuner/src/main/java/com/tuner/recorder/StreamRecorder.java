package com.tuner.recorder;

import com.tuner.settings.SettingsProvider;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@Component
public class StreamRecorder {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(StreamRecorder.class);

    @Value("${tvheadened.username}")
    String username;
    @Value("${tvheadened.password}")
    String password;
    @Value("${recording.location}")
    String location;

    byte[] buffer = new byte[102400];
    boolean allowedRecording = false;
    boolean recording = false;
    int size = 0;
    long startTime;

    public StreamRecorder(@Autowired SettingsProvider settingsProvider) {
        settingsProvider.subscribe("recording.location", c -> location = c);
    }

    public void start(String filename, String url) {
        recording = true;
        new Thread(() -> {
            try {
                recordInternal(filename, url);
            } catch (IOException | InterruptedException e) { //TODO: Handle exceptions
                e.printStackTrace();
            }
        }).start();
        startTime = System.currentTimeMillis();
    }

    public void recordInternal(String filename, String url) throws IOException, InterruptedException {
        InputStream stream = getStream(url);
        allowedRecording = true;

        File file = new File(location + "/" + filename);
        if (file.exists()) {
            file.delete();
        }

        logger.debug("started recording");
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
        logger.info("stopped recording");

    }

    public void stop() {
        allowedRecording = false;
    }

    public int getSize() {
        return size;
    }

    public boolean isRecording() {
        return recording;
    }

    public int recordingTimeInSeconds() {
        if (!recording) {
            return 0;
        }

        long millis = System.currentTimeMillis() - startTime;
        return (int) (millis / 1000);
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
