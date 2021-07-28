package com.tuner.persistence.db;

import com.tuner.model.server_requests.RecordedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordedFileRepository extends JpaRepository<RecordedFile, Integer> {
}
