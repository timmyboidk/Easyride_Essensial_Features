package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final RestTemplate restTemplate;

    @Value("${service-urls.user-service}")
    private String userServiceBaseUrl;

    @Autowired
    public AdminUserServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public UserPageDto_FromUserService listUsers(int page, int size, String role, String searchTerm) {
        log.info("Attempting to list users with params: page={}, size={}, role={}, searchTerm={}", page, size, role, searchTerm);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl + "/admin/list")
                .queryParam("page", page)
                .queryParam("size", size);
        if (role != null && !role.isEmpty()) {
            builder.queryParam("role", role);
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            builder.queryParam("searchTerm", searchTerm);
        }

        String url = builder.toUriString();
        log.debug("Executing GET request to URL: {}", url);

        try {
            // Use ParameterizedTypeReference to capture the generic type information for ApiResponse<UserPageDto_FromUserService>
            ResponseEntity<ApiResponse<UserPageDto_FromUserService>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<UserPageDto_FromUserService>>() {}
            );

            ApiResponse<UserPageDto_FromUserService> apiResponse = responseEntity.getBody();

            if (responseEntity.getStatusCode() != HttpStatus.OK || apiResponse == null) {
                log.error("Received non-OK status or null body from User Service. Status: {}", responseEntity.getStatusCode());
                throw new ExternalServiceException("Failed to fetch users: Invalid response from server.");
            }
            if (apiResponse.getCode() != 0) {
                log.error("User Service returned a business error. Code: {}, Message: {}", apiResponse.getCode(), apiResponse.getMessage());
                throw new ExternalServiceException("Failed to fetch users: " + apiResponse.getMessage());
            }

            log.info("Successfully fetched user list.");
            return apiResponse.getData();
        } catch (RestClientException e) {
            log.error("Error while calling User Service for listUsers at URL: {}", url, e);
            throw new ExternalServiceException("Unable to connect to User Service: " + e.getMessage());
        }
    }

    @Override
    public UserDetailDto_FromUserService getUserDetails(Long userId) {
        log.info("Attempting to get details for user ID: {}", userId);
        String url = userServiceBaseUrl + "/internal/" + userId;
        log.debug("Executing GET request to URL: {}", url);

        try {
            ResponseEntity<ApiResponse<UserDetailDto_FromUserService>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<UserDetailDto_FromUserService>>() {}
            );

            ApiResponse<UserDetailDto_FromUserService> apiResponse = responseEntity.getBody();

            if (responseEntity.getStatusCode() != HttpStatus.OK || apiResponse == null) {
                log.error("Received non-OK status or null body for user details. Status: {}", responseEntity.getStatusCode());
                throw new ExternalServiceException("Failed to get user details: Invalid response from server.");
            }
            if (apiResponse.getCode() != 0) {
                log.error("User Service returned a business error for user details. Code: {}, Message: {}", apiResponse.getCode(), apiResponse.getMessage());
                throw new ExternalServiceException("Failed to get user details: " + apiResponse.getMessage());
            }

            log.info("Successfully fetched details for user ID: {}", userId);
            return apiResponse.getData();
        } catch (RestClientException e) {
            log.error("Error while calling User Service for user details at URL: {}", url, e);
            throw new ExternalServiceException("Unable to connect to User Service for user details: " + e.getMessage());
        }
    }

    @Override
    public UserDetailDto_FromUserService updateUserProfile(Long userId, AdminUserProfileUpdateDto updateDto) {
        log.info("Attempting to update profile for user ID: {}", userId);
        String url = userServiceBaseUrl + "/admin/" + userId + "/profile";
        log.debug("Executing PUT request to URL: {}", url);

        try {
            HttpEntity<AdminUserProfileUpdateDto> requestEntity = new HttpEntity<>(updateDto);
            ResponseEntity<ApiResponse<UserDetailDto_FromUserService>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<ApiResponse<UserDetailDto_FromUserService>>() {}
            );

            ApiResponse<UserDetailDto_FromUserService> apiResponse = responseEntity.getBody();

            if (responseEntity.getStatusCode() != HttpStatus.OK || apiResponse == null) {
                log.error("Received non-OK status or null body on profile update. Status: {}", responseEntity.getStatusCode());
                throw new ExternalServiceException("Failed to update profile: Invalid response from server.");
            }
            if (apiResponse.getCode() != 0) {
                log.error("User Service returned a business error on profile update. Code: {}, Message: {}", apiResponse.getCode(), apiResponse.getMessage());
                throw new ExternalServiceException("Failed to update profile: " + apiResponse.getMessage());
            }

            log.info("Successfully updated profile for user ID: {}", userId);
            return apiResponse.getData();
        } catch (RestClientException e) {
            log.error("Error while calling User Service to update profile at URL: {}", url, e);
            throw new ExternalServiceException("Unable to connect to User Service to update profile: " + e.getMessage());
        }
    }

    @Override
    public void enableUser(Long userId) {
        log.info("Attempting to enable user ID: {}", userId);
        String url = userServiceBaseUrl + "/admin/" + userId + "/enable";
        performPostAction(url, "enable");
        log.info("Successfully sent request to enable user ID: {}", userId);
    }

    @Override
    public void disableUser(Long userId) {
        log.info("Attempting to disable user ID: {}", userId);
        String url = userServiceBaseUrl + "/admin/" + userId + "/disable";
        performPostAction(url, "disable");
        log.info("Successfully sent request to disable user ID: {}", userId);
    }

    // Helper method to reduce code duplication for simple POST actions
    private void performPostAction(String url, String actionName) {
        log.debug("Executing POST request to URL: {}", url);
        try {
            ResponseEntity<ApiResponse<Void>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            ApiResponse<Void> apiResponse = responseEntity.getBody();
            if (responseEntity.getStatusCode() != HttpStatus.OK || apiResponse == null) {
                throw new ExternalServiceException(String.format("Failed to %s user: Invalid response from server.", actionName));
            }
            if (apiResponse.getCode() != 0) {
                throw new ExternalServiceException(String.format("Failed to %s user: %s", actionName, apiResponse.getMessage()));
            }
        } catch (RestClientException e) {
            log.error("Error while calling User Service to {} user at URL: {}", actionName, url, e);
            throw new ExternalServiceException(String.format("Unable to connect to User Service to %s user: %s", actionName, e.getMessage()));        }
    }
}