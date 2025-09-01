package com.easyride.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
public class Driver extends User {

    private String driverLicenseNumber;
    private String vehicleInfo; // e.g., "Toyota Camry 2020, Plate XYZ123"

    // Paths or identifiers for stored documents
    private String driverLicenseDocumentPath;
    private String vehicleDocumentPath;
    // Potentially other documents: insurance, background check results path etc.

    @Enumerated(EnumType.STRING)
    private DriverApprovalStatus approvalStatus = DriverApprovalStatus.PENDING_SUBMISSION; // Default

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    public Driver(String username, String password, String email, String phoneNumber) {
        super();
        this.approvalStatus = DriverApprovalStatus.PENDING_SUBMISSION; // Or PENDING_REVIEW after DTO is processed
    }

    public void setVerificationStatus(DriverApprovalStatus verificationStatus) {
    }
}
