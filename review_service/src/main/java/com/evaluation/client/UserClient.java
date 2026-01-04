package com.evaluation.client;

import com.evaluation.dto.UserDTO; // Ensure this DTO matches what User Service provides
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Name should be the service name as registered in service discovery (e.g., Eureka, Consul)
// Or provide a direct URL for local development (not recommended for prod)
@FeignClient(name = "user-service" /*, url = "${user.service.url}" */)
public interface UserClient {

    @GetMapping("/users/internal/{userId}") // Assuming User Service has an internal endpoint like this
    UserDTO getUserById(@PathVariable("userId") Long userId);

    // Add other methods if needed, e.g., to get user roles or specific details
}