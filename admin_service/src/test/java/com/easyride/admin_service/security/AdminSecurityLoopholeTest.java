package com.easyride.admin_service.security;

import com.easyride.admin_service.controller.AdminDriverManagementController;
import com.easyride.admin_service.controller.AdminUserController;
import org.junit.jupiter.api.Test;
// import org.springframework.security.access.prepost.PreAuthorize; 
// PreAuthorize might not be on classpath if security dependencies are minimalist or provided, 
// but usually it is. We will use reflection with string name to be safe if import fails.

import java.lang.annotation.Annotation;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSecurityLoopholeTest {

    @Test
    void testAdminUserController_MissingSecurityAnnotation() {
        boolean hasPreAuthorize = hasPreAuthorizeAnnotation(AdminUserController.class);

        // Fix Verification: We now EXPECT this to be true.
        assertTrue(hasPreAuthorize, "AdminUserController should have @PreAuthorize annotation.");
    }

    @Test
    void testAdminDriverManagementController_MissingSecurityAnnotation() {
        boolean hasPreAuthorize = hasPreAuthorizeAnnotation(AdminDriverManagementController.class);

        assertTrue(hasPreAuthorize, "AdminDriverManagementController should have @PreAuthorize annotation.");
    }

    private boolean hasPreAuthorizeAnnotation(Class<?> clazz) {
        // checks class level
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals("PreAuthorize")) {
                return true;
            }
        }
        // check method level (if any method has it, we might consider it partially
        // secured, but we want class level here)
        return false;
    }
}
