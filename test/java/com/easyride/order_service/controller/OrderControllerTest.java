package com.easyride.order_service.controller;

import com.easyride.order_service.dto.LocationDto;
import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.exception.OrderServiceException; // 导入自定义异常
import com.easyride.order_service.interceptor.SignatureVerificationInterceptor;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.model.PaymentMethod;
import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.model.VehicleType;
import com.easyride.order_service.security.JwtTokenProvider;
import com.easyride.order_service.security.OrderDetailServiceImpl;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@WithMockUser(username = "testuser", roles = {"PASSENGER"}) // 将认证信息应用到整个测试类
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;
    @MockBean
    private SignatureVerificationInterceptor signatureVerificationInterceptor;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private OrderDetailServiceImpl orderDetailService;

    @BeforeEach
    void setUp() throws Exception {
        when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // --- “快乐路径”测试 (Happy Path) ---
    @Test
    void createOrder_whenValidInput_shouldReturn200AndOrderResponse() throws Exception {
        // ... 此测试用例与之前完全相同，保持不变 ...
        OrderCreateDto orderCreateDto = createValidOrderCreateDto(); // 使用辅助方法创建对象
        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setOrderId(100L);
        orderResponseDto.setStatus(OrderStatus.PENDING_MATCH);
        given(orderService.createOrder(any(OrderCreateDto.class))).willReturn(orderResponseDto);

        mockMvc.perform(post("/orders/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderCreateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(100L));
    }


    // --- 边缘情况1：输入校验失败 (Bean Validation) ---
    // 我们使用参数化测试，一次性测试所有字段为空的情况，让代码更简洁
    @ParameterizedTest
    @MethodSource("provideInvalidOrderCreateDtos")
    void createOrder_whenInputIsInvalid_shouldReturn400(OrderCreateDto invalidDto, String expectedErrorMessage) throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest()) // 断言：HTTP状态码为 400 (Bad Request)
                .andExpect(content().string(expectedErrorMessage)); // 断言：响应体内容为我们预期的错误信息
    }

    // 为参数化测试提供数据源
    private static Stream<Arguments> provideInvalidOrderCreateDtos() {
        return Stream.of(
                Arguments.of(withPassengerId(null), "乘客ID不能为空"),
                Arguments.of(withStartLocation(null), "起始位置不能为空"),
                Arguments.of(withEndLocation(null), "结束位置不能为空"),
                Arguments.of(withVehicleType(null), "车辆类型不能为空"),
                Arguments.of(withServiceType(null), "服务类型不能为空"),
                Arguments.of(withPaymentMethod(null), "支付方式不能为空")
        );
    }


    // --- 边缘情况2：Service层抛出业务异常 ---
    @Test
    void createOrder_whenServiceThrowsException_shouldReturn400() throws Exception {
        // 1. 准备 (Arrange)
        // 模拟当 service.createOrder 被调用时，抛出一个自定义的业务异常
        String errorMessage = "预约时间必须至少在当前时间15分钟后";
        given(orderService.createOrder(any(OrderCreateDto.class)))
                .willThrow(new OrderServiceException(errorMessage));

        OrderCreateDto orderCreateDto = createValidOrderCreateDto();

        // 2. 执行 (Act) & 3. 断言 (Assert)
        mockMvc.perform(post("/orders/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderCreateDto)))
                .andExpect(status().isBadRequest()) // 断言：捕获到异常后，返回 400
                .andExpect(content().string(errorMessage)); // 断言：响应体是异常信息
    }


    // --- 辅助方法 (Helper Methods) ---
    // 后面这些都是辅助方法，用于快速创建测试对象，让测试代码更清晰

    private static OrderCreateDto createValidOrderCreateDto() {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setPassengerId(1L);
        dto.setStartLocation(new LocationDto(34.0, -118.0));
        dto.setEndLocation(new LocationDto(34.1, -118.1));
        dto.setVehicleType(VehicleType.STANDARD);
        dto.setServiceType(ServiceType.NORMAL);
        dto.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        return dto;
    }

    private static OrderCreateDto withPassengerId(Long id) {
        OrderCreateDto dto = createValidOrderCreateDto();
        dto.setPassengerId(id);
        return dto;
    }

    private static OrderCreateDto withStartLocation(LocationDto location) {
        OrderCreateDto dto = createValidOrderCreateDto();
        dto.setStartLocation(location);
        return dto;
    }

    private static OrderCreateDto withEndLocation(LocationDto location) {
        OrderCreateDto dto = createValidOrderCreateDto();
        dto.setEndLocation(location);
        return dto;
    }

    private static OrderCreateDto withVehicleType(VehicleType type) {
        OrderCreateDto dto = createValidOrderCreateDto();
        dto.setVehicleType(type);
        return dto;
    }

    private static OrderCreateDto withServiceType(ServiceType type) {
        OrderCreateDto dto = createValidOrderCreateDto();
        dto.setServiceType(type);
        return dto;
    }

    private static OrderCreateDto withPaymentMethod(PaymentMethod method) {
        OrderCreateDto dto = createValidOrderCreateDto();
        dto.setPaymentMethod(method);
        return dto;
    }
}