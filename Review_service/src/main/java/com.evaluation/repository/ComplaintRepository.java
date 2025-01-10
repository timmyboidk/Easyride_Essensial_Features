package com.evaluation.repository;

import com.evaluation.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByEvaluationId(Long evaluationId);
    List<Complaint> findByComplainantId(Long complainantId);
}
