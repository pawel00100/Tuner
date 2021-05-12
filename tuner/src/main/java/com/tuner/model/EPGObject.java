package com.tuner.model;

import java.util.List;

public class EPGObject {
    int totalCount;
    List<EPGEvent> entries;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<EPGEvent> getEntries() {
        return entries;
    }

    public void setEntries(List<EPGEvent> entries) {
        this.entries = entries;
    }
}
