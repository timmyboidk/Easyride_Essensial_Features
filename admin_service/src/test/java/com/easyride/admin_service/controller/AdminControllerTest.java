package com.easyride.admin_service.controller;

import com.easyride.admin_service.dto.AdminOrderInterveneEvent;
import com.easyride.admin_service.dto.AdminUserDto;
import com.easyride.admin_service.model.AdminUser;
import com.easyride.admin_service.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AdminService adminService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void createAdminUser_Success() throws Exception {
                AdminUserDto dto = AdminUserDto.builder()
                                .username("newAdmin")
                                .password("password")
                                .role("OPERATOR")
                                .build();

                AdminUser user = new AdminUser();
                user.setUsername("newAdmin");
                user.setId(1L);

                when(adminService.createAdminUser(any(AdminUserDto.class))).thenReturn(user);

                mockMvc.perform(post("/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("newAdmin"));
        }

        @Test
        void updateAdminUser_Success() throws Exception {
                AdminUserDto dto = AdminUserDto.builder()
                                .id(1L)
                                .username("updatedAdmin")
                                .build();

                AdminUser user = new AdminUser();
                user.setUsername("updatedAdmin");
                user.setId(1L);

                when(adminService.updateAdminUser(any(AdminUserDto.class))).thenReturn(user);

                mockMvc.perform(put("/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("updatedAdmin"));
        }

        @Test
        void disableAdminUser_Success() throws Exception {
                doNothing().when(adminService).disableAdminUser(1L);

                mockMvc.perform(post("/admin/users/1/disable"))
                                .andExpect(status().isOk());
        }

        @Test
        void interveneOrder_Success() throws Exception {
                AdminOrderInterveneEvent event = AdminOrderInterveneEvent.builder()
                                .orderId(100L)
                                .action("CANCEL")
                                .reason("Fraud")
                                .build();

                doNothing().when(adminService).interveneOrder(any(AdminOrderInterveneEvent.class));

                mockMvc.perform(post("/admin/orders/intervene")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(event)))
                                .andExpect(status().isOk());
        }
}
