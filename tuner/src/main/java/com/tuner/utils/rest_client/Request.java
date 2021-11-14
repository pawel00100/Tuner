package com.tuner.utils.rest_client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;


public class Request {
    private static final ObjectMapper mapper = new ObjectMapper();
    private HttpRequest.Builder requestBuilder;


    public Request(URI uri) {
        requestBuilder = HttpRequest.newBuilder(uri);
    }

    public Request post(Object object) throws JsonProcessingException {
        addHeader("Content-Type", "application/json");
        return post(mapper.writeValueAsString(object));
    }

    public Request post(String body) {
        requestBuilder = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
        return this;
    }

    public Request GET() {
        requestBuilder = requestBuilder.GET();
        return this;
    }

    public Request addHeader(String name, String value) {
        requestBuilder = requestBuilder.header(name, value);
        return this;
    }

    public Request basicAuth(String user, String password) {
        return addHeader("Authorization", authHeader(user, password));
    }

    public Request auth(Credentials credentials) {
        return switch (credentials.authType) {
            case BASIC -> basicAuth(credentials.username(), credentials.password());
        };
    }

    public HttpRequest getRequest() {
        return requestBuilder.build();
    }

    public RestClient build(HttpClient httpClient) {
        return new RestClient(httpClient, requestBuilder.build());
    }

    public RestClient build() {
        return new RestClient(requestBuilder.build());
    }

    private static String authHeader(String user, String password) {
        String auth = user + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    public enum AuthType {
        BASIC
    }

    public static record Credentials(String username, String password, AuthType authType) {
    }
}
