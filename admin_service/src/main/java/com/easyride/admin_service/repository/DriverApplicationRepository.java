package com.easyride.admin_service.repository;

import com.easyride.admin_service.model.DriverApplication;
import com.easyride.admin_service.model.DriverApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverApplicationRepository extends JpaRepository<DriverApplication, Long> {
    Page<DriverApplication> findByStatus(DriverApplicationStatus status, Pageable pageable);
}