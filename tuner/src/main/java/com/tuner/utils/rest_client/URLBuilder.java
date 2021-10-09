package com.tuner.utils.rest_client;


import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

public class URLBuilder {

    private URIBuilder uriBuilder;

    public URLBuilder(String string) throws URISyntaxException {
        uriBuilder = new URIBuilder(string);
    }

    public URLBuilder setParameter(String param, String value) {
        uriBuilder = uriBuilder.setParameter(param, value);
        return this;
    }


    public URI getURI() throws URISyntaxException {
        return uriBuilder.build();
    }

    public Request build() throws URISyntaxException {
        return new Request(uriBuilder.build());
    }

}
