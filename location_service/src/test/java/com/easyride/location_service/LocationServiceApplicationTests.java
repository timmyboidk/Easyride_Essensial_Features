package com.easyride.location_service;

import com.easyride.location_service.controller.LocationController;
import com.easyride.location_service.service.LocationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled("Requires database connection - use integration tests with Testcontainers instead")
public class LocationServiceApplicationTests {

    @Autowired
    private LocationController locationController;

    @MockitoBean
    private LocationService locationService;

    @Test
    public void contextLoads() {
        // 验证控制器是否成功加载
        assertThat(locationController).isNotNull();
    }
}
