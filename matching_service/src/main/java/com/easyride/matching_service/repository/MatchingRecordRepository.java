package com.easyride.matching_service.repository;

import com.easyride.matching_service.model.MatchingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRecordRepository extends JpaRepository<MatchingRecord, Long> {
}
