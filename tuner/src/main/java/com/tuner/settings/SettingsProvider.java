package com.tuner.settings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class SettingsProvider {

    Multimap<String, Consumer<String>> observers = ArrayListMultimap.create();


    public SettingsProvider() {

    }

    public void subscribe(String property, Consumer<String> consumer) {
        observers.put(property, consumer);
    }

    public void set(String key, String value) {
        observers.get(key).forEach(c -> c.accept(value));
    }

}
