package com.easyride.analytics_service.model;

public enum RecordType {
    ORDER_REVENUE,
    COMPLETED_ORDERS_COUNT,
    CANCELLED_ORDERS_COUNT, // New

    USER_REGISTRATION,      // New
    DRIVER_REGISTRATION,    // New
    DRIVER_APPROVED,        // New

    PAYMENT_TRANSACTION,    // New (can have value for amount)
    PAYMENT_FAILED,         // New (count)
    PAYMENT_REFUNDED,       // New (count, and value for amount)

    REVIEW_SUBMITTED,       // New (count)
    AVERAGE_RATING_PASSENGER, // New (value is the rating)
    AVERAGE_RATING_DRIVER,  // New (value is the rating)

    ACTIVE_USER_LOGIN,      // New (for DAU/MAU tracking, value might be userId for HyperLogLog)
    DRIVER_ONLINE_SESSION,  // New (value could be session duration in minutes)

    ORDER_REQUEST,          // New (count of all order requests)
    ORDER_ACCEPTED_BY_DRIVER, // New (count of orders accepted by drivers)

    // Composite metrics might not be direct RecordTypes but calculated from these.
    // Example: DRIVER_ACCEPTANCE_RATE would be (ORDER_ACCEPTED_BY_DRIVER / ORDER_REQUESTS_TO_DRIVERS)
}