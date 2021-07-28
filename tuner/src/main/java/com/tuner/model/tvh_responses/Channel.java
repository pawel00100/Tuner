package com.tuner.model.tvh_responses;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Channel {
    String id;
    String name;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private int dbId;

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
