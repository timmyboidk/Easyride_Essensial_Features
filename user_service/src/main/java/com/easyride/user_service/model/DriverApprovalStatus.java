package com.easyride.user_service.model;

public enum DriverApprovalStatus {
    PENDING_SUBMISSION, // Initial state, documents might still be pending
    PENDING_REVIEW,     // All required info submitted, awaiting admin review
    APPROVED,
    REJECTED,
    NEEDS_RESUBMISSION  // If some documents were invalid
}