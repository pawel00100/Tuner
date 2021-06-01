package com.tuner.model.tvh_responses;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class Channel {
    String id;
    String name;

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonSetter("key")
    public void setId(String id) {
        this.id = id;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("val")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }
}
