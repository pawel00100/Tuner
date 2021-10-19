package com.tuner.model.server_requests;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
public class Channel {
    String id;
    String name;
    @JsonProperty("multiplex_name")
    String multiplex;
    @JsonProperty("multiplex_id")
    String multiplexID;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private int dbId;

    public Channel() {
    }

    public Channel(String id, String name, String multiplex, String multiplexID) {
        this.id = id;
        this.name = name;
        this.multiplex = multiplex;
        this.multiplexID = multiplexID;
    }

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }
}
