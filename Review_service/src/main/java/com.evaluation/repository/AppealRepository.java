package com.evaluation.repository;

import com.evaluation.model.Appeal;
import com.evaluation.model.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, Long> {
    Optional<Appeal> findByComplaintId(Long complaintId);
    Optional<Appeal> findByEvaluationIdAndAppellantId(Long evaluationId, Long appellantId); // If multiple appeals per eval are not allowed
    Page<Appeal> findByStatus(AppealStatus status, Pageable pageable);
}