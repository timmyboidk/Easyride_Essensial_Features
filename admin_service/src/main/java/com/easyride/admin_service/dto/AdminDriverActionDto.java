package com.easyride.admin_service.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * DTO for capturing admin actions on a driver application, such as approving or rejecting.
 * This is used as the request body in the AdminDriverManagementController.
 */
@Data
public class AdminDriverActionDto {

    /**
     * The reason for the action, especially required when rejecting an application.
     */
    @NotEmpty(message = "Reason cannot be empty when rejecting.")
    private String reason;

    /**
     * Optional administrative notes for logging or internal records.
     */
    private String notes;
}