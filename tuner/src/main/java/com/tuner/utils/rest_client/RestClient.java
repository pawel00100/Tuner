package com.tuner.utils.rest_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;


public class RestClient {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String url = "http://raspberrypi:9981/api"; //TODO: as property
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpRequest httpRequestNoAuth(String endpoint, String parsedObject, String type) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url + endpoint))
                .method(type, HttpRequest.BodyPublishers.ofString(parsedObject))
                .header("Content-Type", "application/json")
                .build();
    }

}
