package com.evaluation.model;

public enum EvaluationStatus {
    ACTIVE,         // Visible to users
    UNDER_REVIEW,   // Flagged, pending admin review
    HIDDEN_BY_ADMIN,// Hidden by admin (e.g., inappropriate)
    RESOLVED        // If there was an issue and it's resolved
}