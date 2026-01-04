package com.easyride.admin_service.controller;

import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void listPlatformUsers_Success() {
        ReflectionTestUtils.setField(adminUserController, "defaultPageSize", 20);
        UserPageDto_FromUserService userPage = new UserPageDto_FromUserService();
        when(adminUserService.listUsers(anyInt(), anyInt(), any(), any())).thenReturn(userPage);

        ApiResponse<UserPageDto_FromUserService> response = adminUserController.listPlatformUsers(0, 10, "PASSENGER",
                null);
        assertEquals(0, response.getCode());
    }

    @Test
    void getPlatformUserDetails_Success() {
        UserDetailDto_FromUserService userDetail = new UserDetailDto_FromUserService();
        when(adminUserService.getUserDetails(anyLong())).thenReturn(userDetail);

        ApiResponse<UserDetailDto_FromUserService> response = adminUserController.getPlatformUserDetails(1L);
        assertEquals(0, response.getCode());
    }

    @Test
    void updatePlatformUserProfile_Success() {
        UserDetailDto_FromUserService updatedUser = new UserDetailDto_FromUserService();
        when(adminUserService.updateUserProfile(anyLong(), any(AdminUserProfileUpdateDto.class)))
                .thenReturn(updatedUser);

        ApiResponse<UserDetailDto_FromUserService> response = adminUserController.updatePlatformUserProfile(1L,
                new AdminUserProfileUpdateDto());
        assertEquals(0, response.getCode());
    }

    @Test
    void enablePlatformUser_Success() {
        doNothing().when(adminUserService).enableUser(anyLong());

        ApiResponse<Void> response = adminUserController.enablePlatformUser(1L);
        assertEquals(0, response.getCode());
    }

    @Test
    void disablePlatformUser_Success() {
        doNothing().when(adminUserService).disableUser(anyLong());

        ApiResponse<Void> response = adminUserController.disablePlatformUser(1L);
        assertEquals(0, response.getCode());
    }
}
