package com.tuner.model.tvh_responses;

import lombok.Data;

import java.util.List;

@Data
public class EPGObject {
    int totalCount;
    List<EPGEvent> entries;

}
