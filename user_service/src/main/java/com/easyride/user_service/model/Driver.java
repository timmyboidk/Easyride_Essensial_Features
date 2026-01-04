package com.easyride.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getIdCardFrontUrl() {
        return idCardFrontUrl;
    }

    public void setIdCardFrontUrl(String idCardFrontUrl) {
        this.idCardFrontUrl = idCardFrontUrl;
    }

    public String getIdCardBackUrl() {
        return idCardBackUrl;
    }

    public void setIdCardBackUrl(String idCardBackUrl) {
        this.idCardBackUrl = idCardBackUrl;
    }

    public String getDriverLicenseNumber() {
        return driverLicenseNumber;
    }

    public void setDriverLicenseNumber(String driverLicenseNumber) {
        this.driverLicenseNumber = driverLicenseNumber;
    }

    public String getDriverLicenseUrl() {
        return driverLicenseUrl;
    }

    public void setDriverLicenseUrl(String driverLicenseUrl) {
        this.driverLicenseUrl = driverLicenseUrl;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getCarLicensePlate() {
        return carLicensePlate;
    }

    public void setCarLicensePlate(String carLicensePlate) {
        this.carLicensePlate = carLicensePlate;
    }

    public String getCarInsuranceUrl() {
        return carInsuranceUrl;
    }

    public void setCarInsuranceUrl(String carInsuranceUrl) {
        this.carInsuranceUrl = carInsuranceUrl;
    }

    public DriverApprovalStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Double getServiceRatingAvg() {
        return serviceRatingAvg;
    }

    public void setServiceRatingAvg(Double serviceRatingAvg) {
        this.serviceRatingAvg = serviceRatingAvg;
    }

    public void setVerificationStatus(DriverApprovalStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
