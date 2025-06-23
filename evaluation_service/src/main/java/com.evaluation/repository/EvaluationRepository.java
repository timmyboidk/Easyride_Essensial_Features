package com.evaluation.repository;

import com.evaluation.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByEvaluateeId(Long evaluateeId);
    List<Evaluation> findByEvaluatorId(Long evaluatorId);
}
