package com.tuner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.tuner.model.server_requests.RecordedFile;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@ActiveProfiles("integrationtest")
public class IntegrationTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    @Rule
    static WireMockRule wireMockRule = new WireMockRule(
            WireMockConfiguration.wireMockConfig().port(9090));
    private static WireMockServer wireMockServer;
    @Value("${recording.location}")
    String location;

    @BeforeAll
    public static void run() throws IOException {
        long timestamp = System.currentTimeMillis() / 1000;

        //TVH get
        var channelList = Files.readString(Path.of("./src/test/resources/com/tuner/integrationtest/tvhresponses/channel_list.json"));
        var epgEventsGrid = Files.readString(Path.of("./src/test/resources/com/tuner/integrationtest/tvhresponses/epg_events_grid.json"))
                .replaceAll("start1", Long.toString((timestamp - 1)))
                .replaceAll("end1", Long.toString((timestamp + 20)));
        var servicesGrid = Files.readString(Path.of("./src/test/resources/com/tuner/integrationtest/tvhresponses/services_grid.json"));
        //Server get
        var orders = Files.readString(Path.of("./src/test/resources/com/tuner/integrationtest/serverresponses/orders.json"))
                .replaceAll("start1", Long.toString((timestamp - 1)))
                .replaceAll("end1", Long.toString((timestamp + 20)));
        var settings = Files.readString(Path.of("./src/test/resources/com/tuner/integrationtest/serverresponses/settings.json"));


        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();

        configureFor("127.0.0.1", 8089);

        //TVH get
        mockResponse(epgEventsGrid, "/api/epg/events/grid.*");
        mockResponse(channelList, "/api/channel/list");
        mockResponse(servicesGrid, "/api/mpegts/service/grid");
        mockResponse("Binary content".getBytes(), "/stream/channel/.*");
        //Server get
        mockResponse(orders, "/orders.*");
        mockResponse(settings, "/settings.*");
        //Server post
        mockResponse("/channels.*");
        mockResponse("/epg.*");
        mockResponse("/recorded.*");

    }

    @AfterAll
    public static void after() {
        wireMockServer.stop();
    }

    @Test
    public void test() throws IOException, InterruptedException {
        deleteRecordingsDirectory();

        var start = System.currentTimeMillis();

        boolean recorded;
        do {
            Thread.sleep(500);
            List<LoggedRequest> str = findAll(postRequestedFor(urlPathMatching("/recorded.*")));
            List<String> str2 = str.stream().map(lr -> new String(lr.getBody())).toList();
            recorded = twoElements(str2);
        } while (!recorded && (System.currentTimeMillis() - start) < 25_000);


        //TVH get
        verify(moreThan(0), getRequestedFor(urlPathMatching("/api/epg/events/grid.*")));
        verify(moreThan(0), getRequestedFor(urlPathMatching("/api/channel/list")));
        verify(moreThan(0), getRequestedFor(urlPathMatching("/api/mpegts/service/grid")));
        verify(moreThan(0), getRequestedFor(urlPathMatching("/stream/channel/.*")));
        verify(moreThan(0), getRequestedFor(urlPathMatching("/stream/channel/b9fe4a8b90722d1da9fccfb45bb13419")));
        verify(moreThan(0), getRequestedFor(urlPathMatching("/stream/channel/2538c2b55f8f937dd6a8d6bfe4049d3b")));
        //Server get
        verify(moreThan(0), getRequestedFor(urlPathMatching("/orders.*")));
        //Server post
        verify(moreThan(0), postRequestedFor(urlPathMatching("/channels.*")));
        verify(moreThan(0), postRequestedFor(urlPathMatching("/epg.*")));
        verify(moreThan(0), postRequestedFor(urlPathMatching("/recorded.*")));


        assertEquals(2, Files.list(Paths.get(location)).count());
        FileSystemUtils.deleteRecursively(Paths.get(location));

        deleteRecordingsDirectory();
    }

    private boolean twoElements(List<String> requests) throws JsonProcessingException {
        List<RecordedFile> recordings = null;
        for (var req : requests) {
            recordings = mapper.readValue(req, new TypeReference<>() {
            });
            var uniqueCount = recordings.stream().map(RecordedFile::getChannelId).distinct().count();
            if (uniqueCount == 2) {
                return true;
            }
        }
        return false;
    }

    private void deleteRecordingsDirectory() throws IOException {
        if (Files.isDirectory(Paths.get(location))) {
            FileSystemUtils.deleteRecursively(Paths.get(location));
        }
    }

    private static void mockResponse(String orders, String s) {
        stubFor(any(urlPathMatching(s))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/json")
                        .withStatus(200)
                        .withBody(orders)));
    }

    private static void mockResponse(byte[] orders, String s) {
        stubFor(any(urlPathMatching(s))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/json")
                        .withStatus(200)
                        .withBody(orders)));
    }

    private static void mockResponse(String s) {
        stubFor(any(urlPathMatching(s))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/json")
                        .withStatus(200)));
    }

}