package com.easyride.location_service;

import com.easyride.location_service.controller.LocationController;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.service.LocationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LocationServiceApplicationTests {

    @Autowired
    private LocationController locationController;

    @MockBean
    private LocationService locationService;

    @Test
    public void contextLoads() {
        // 验证控制器是否成功加载
        assertThat(locationController).isNotNull();
    }

    @Test
    public void testGetLocationInfo() {
        // 准备模拟的响应数据
        LocationResponse mockResponse = new LocationResponse();
        mockResponse.setStatus("OK");
        // 根据您的实际模型，设置其他必要的字段
        // 例如：mockResponse.setResults(...);

        // 模拟LocationService的方法
        Mockito.when(locationService.getLocationInfo(37.4224764, -122.0842499))
                .thenReturn(mockResponse);

        // 调用控制器方法
        LocationResponse response = locationController.getLocationInfo(37.4224764, -122.0842499);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");
    }
}
