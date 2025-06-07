package com.evaluation.model;

public enum AppealStatus {
    PENDING_REVIEW,
    UNDER_INVESTIGATION,
    UPHELD, // Appeal successful, original decision might be overturned
    REJECTED // Appeal denied
}