package com.tuner.utils.rest_client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;

public class Response {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final HttpResponse<String> httpResponse;


    public Response(HttpResponse<String> httpResponse) {
        this.httpResponse = httpResponse;
    }

    public <T> T deserialize(Class<T> valueType) throws JsonProcessingException {
        return mapper.readValue(httpResponse.body(), valueType);
    }

    public <T> T deserialize(TypeReference<T> valueTypeRef) throws JsonProcessingException {
        return mapper.readValue(httpResponse.body(), valueTypeRef);
    }

    public String getBody() {
        return httpResponse.body();
    }

    public HttpResponse<String> getResponse() {
        return httpResponse;
    }

    public Response assertStatusCodeOK() throws RequestException {
        if (is2xx()) {
            throw new RequestException("Got status code: " + httpResponse.statusCode() + " response body: " + httpResponse.body());
        }
        return this;
    }

    private boolean is2xx() {
        return (httpResponse.statusCode() / 100) != 2;
    }

}
