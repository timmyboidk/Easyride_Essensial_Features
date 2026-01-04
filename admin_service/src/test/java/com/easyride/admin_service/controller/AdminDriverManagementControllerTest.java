package com.easyride.admin_service.controller;

import com.easyride.admin_service.dto.AdminDriverActionDto;
import com.easyride.admin_service.dto.DriverApplicationDto;
import com.easyride.admin_service.service.AdminDriverManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDriverManagementController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters
class AdminDriverManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDriverManagementService driverManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getPendingApplications_Success() throws Exception {
        DriverApplicationDto dto = new DriverApplicationDto();
        dto.setDriverId(1L);
        dto.setUsername("driver1");
        Page<DriverApplicationDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(driverManagementService.getPendingDriverApplications(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/admin/drivers/applications/pending")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("driver1"));
    }

    @Test
    void getApplicationDetails_Success() throws Exception {
        DriverApplicationDto dto = new DriverApplicationDto();
        dto.setDriverId(1L);
        dto.setUsername("driver1");

        when(driverManagementService.getDriverApplicationDetails(1L)).thenReturn(dto);

        mockMvc.perform(get("/admin/drivers/applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("driver1"));
    }

    @Test
    void approveApplication_Success() throws Exception {
        AdminDriverActionDto action = new AdminDriverActionDto();
        action.setNotes("Approved");
        action.setReason("Good"); // Reason is mandatory for validation, even if approved? DTO validation says
                                  // reason NotEmpty

        doNothing().when(driverManagementService).approveDriverApplication(anyLong(), anyLong(), anyString());

        mockMvc.perform(post("/admin/drivers/applications/1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk()) // Expect 200 based on ApiResponse.success returns
                .andExpect(jsonPath("$.message").value("司机申请已批准"));
    }

    @Test
    void rejectApplication_Success() throws Exception {
        AdminDriverActionDto action = new AdminDriverActionDto();
        action.setReason("Document unclear");
        action.setNotes("Rejected");

        doNothing().when(driverManagementService).rejectDriverApplication(anyLong(), anyLong(), anyString(),
                anyString());

        mockMvc.perform(post("/admin/drivers/applications/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("司机申请已拒绝"));
    }
}
