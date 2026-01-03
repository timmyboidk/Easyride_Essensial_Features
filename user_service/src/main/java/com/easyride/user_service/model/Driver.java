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

    @Column(nullable = false)
    private String realName;

    @Column(nullable = false)
    private String idCardNumber;

    @Column(nullable = false)
    private String idCardFrontUrl;

    @Column(nullable = false)
    private String idCardBackUrl;

    @Column(nullable = false)
    private String driverLicenseNumber;

    @Column(nullable = false)
    private String driverLicenseUrl;

    @Column(nullable = false)
    private String carModel;

    @Column(nullable = false)
    private String carLicensePlate;

    @Column(nullable = false)
    private String carInsuranceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverApprovalStatus verificationStatus = DriverApprovalStatus.PENDING_SUBMISSION;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(precision = 3, scale = 2)
    private Double serviceRatingAvg;

    public Driver(String username, String password, String email, String phoneNumber) {
        super(username, password, email, phoneNumber, Role.DRIVER);
        this.verificationStatus = DriverApprovalStatus.PENDING_SUBMISSION;
    }

    public void setVerificationStatus(DriverApprovalStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
