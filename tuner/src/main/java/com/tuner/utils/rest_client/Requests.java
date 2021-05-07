package com.tuner.utils.rest_client;

import org.apache.commons.codec.binary.Base64;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

public class Requests {

    public static HttpRequest httpRequestWithAuth(String url, String authHeader) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .build();
    }

    public static HttpRequest httpRequestWithAuthWithParams(String url, String authHeader, String key, String val ) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .build();
    }


    public static String getAuthHeader(String user, String password) { //TODO: rework
        String auth = user + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }
}
