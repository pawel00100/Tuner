package com.tuner.persistence.db;

import com.tuner.model.server_requests.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChannelListDAO extends GenericDao<Channel> {
    @Autowired
    ChannelListRepository repository;

    @Override
    public boolean deleteAll() {
        repository.deleteAll();
        return true;
    }
}
