package com.tuner.model.tvh_responses.channel;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.List;

public class TVHChannel {
    String id;
    String name;

    public String getId() {
        return id;
    }

    @JsonSetter("key")
    public void setId(String id) {
        this.id = id;
    }

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

    @Data
    public static class TVHChannelList {
        List<TVHChannel> entries;

        public TVHChannelList(List<TVHChannel> entries) {
            this.entries = entries;
        }

        public TVHChannelList() {
        }
    }
}
