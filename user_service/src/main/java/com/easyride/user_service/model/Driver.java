package com.easyride.user_service.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@TableName("drivers")
@Getter
@Setter
@NoArgsConstructor
public class Driver extends User {
    @TableId
    private Long id;

    private String realName;

    private String idCardNumber;

    private String idCardFrontUrl;

    private String idCardBackUrl;

    private String driverLicenseNumber;

    private String driverLicenseUrl;

    private String carModel;

    private String carLicensePlate;

    private String carInsuranceUrl;

    private DriverApprovalStatus verificationStatus = DriverApprovalStatus.PENDING_SUBMISSION;

    private String reviewNotes;

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
