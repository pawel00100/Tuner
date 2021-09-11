package com.tuner.persistence.db;

import com.tuner.model.server_requests.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelListRepository extends JpaRepository<Channel, Integer> {

    @Modifying
    @Query("DELETE FROM Channel")
    void deleteAll();
}
