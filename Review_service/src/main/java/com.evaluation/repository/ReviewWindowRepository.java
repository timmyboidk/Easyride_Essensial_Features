package com.evaluation.repository;

import com.evaluation.model.ReviewWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewWindowRepository extends JpaRepository<ReviewWindow, Long> {
}