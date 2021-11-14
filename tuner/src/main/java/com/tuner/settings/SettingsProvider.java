package com.tuner.settings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tuner.utils.rest_client.Request;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.function.Consumer;

@Service
public class SettingsProvider {

    @Getter
    @Value("${tvheadened.url}")
    String tvhBaseURL;
    @Getter
    @Value("${tvheadened.username}")
    String tvhUsername;
    @Getter
    @Value("${tvheadened.password}")
    String tvhPassword;

    @Getter
    @Value("${server.url}")
    String serverURL;
    @Getter
    @Value("${server.username}")
    String serverUsername;
    @Getter
    @Value("${server.password}")
    String serverPassword;


    Multimap<String, Consumer<String>> observers = ArrayListMultimap.create();

    public SettingsProvider() {

    }

    public Request.Credentials geTVHCredentials() {
        return new Request.Credentials(tvhUsername, tvhUsername, Request.AuthType.BASIC);
    }

    public void subscribe(String property, Consumer<String> consumer) {
        observers.put(property, consumer);
    }

    public void set(String key, String value) {
        observers.get(key).forEach(c -> c.accept(value));
    }

    public Request.Credentials getServerCredentials() {
        return new Request.Credentials(serverUsername, serverPassword, Request.AuthType.BASIC);
    }

    @PostConstruct
    void postConstruct() {
        subscribe("tvheadened.url", c -> tvhBaseURL = c);
        subscribe("server.url", c -> serverURL = c);
    }
}
