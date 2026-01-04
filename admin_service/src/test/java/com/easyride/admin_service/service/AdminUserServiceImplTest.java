package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminUserService, "userServiceBaseUrl", "http://user-service");
    }

    @Test
    void listUsers_Success() {
        UserPageDto_FromUserService pageDto = new UserPageDto_FromUserService();
        ApiResponse<UserPageDto_FromUserService> apiResponse = ApiResponse.success(pageDto);
        ResponseEntity<ApiResponse<UserPageDto_FromUserService>> responseEntity = new ResponseEntity<>(apiResponse,
                HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<UserPageDto_FromUserService>>>any()))
                .thenReturn(responseEntity);

        UserPageDto_FromUserService result = adminUserService.listUsers(0, 10, "PASSENGER", null);
        assertEquals(pageDto, result);
    }

    @Test
    void getUserDetails_Success() {
        UserDetailDto_FromUserService userDto = new UserDetailDto_FromUserService();
        ApiResponse<UserDetailDto_FromUserService> apiResponse = ApiResponse.success(userDto);
        ResponseEntity<ApiResponse<UserDetailDto_FromUserService>> responseEntity = new ResponseEntity<>(apiResponse,
                HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<UserDetailDto_FromUserService>>>any()))
                .thenReturn(responseEntity);

        UserDetailDto_FromUserService result = adminUserService.getUserDetails(1L);
        assertEquals(userDto, result);
    }

    @Test
    void enableUser_Success() {
        ApiResponse<Void> apiResponse = ApiResponse.success(null);
        ResponseEntity<ApiResponse<Void>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<ApiResponse<Void>>>any())).thenReturn(responseEntity);

        adminUserService.enableUser(1L);
    }
}
