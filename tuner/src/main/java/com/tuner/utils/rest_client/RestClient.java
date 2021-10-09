package com.tuner.utils.rest_client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class RestClient {
    private static final HttpClient sharedHttpClient = HttpClient.newHttpClient();
    private final HttpClient httpClient;
    private final HttpRequest request;


    public RestClient(HttpClient httpClient, HttpRequest request) {
        this.httpClient = httpClient;
        this.request = request;
    }

    public RestClient(HttpRequest request) {
        this.httpClient = sharedHttpClient;
        this.request = request;
    }

    public HttpResponse<String> getResponse() throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public Response send() throws RequestException {
        try {
            return new Response(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException(e);
        }
    }
}
