package com.tuner.model.tvh_responses;

import lombok.Data;

import java.util.List;

@Data
public class TVHChannelList {
    List<Channel> entries;

    public TVHChannelList(List<Channel> entries) {
        this.entries = entries;
    }

    public TVHChannelList() {
    }
}
