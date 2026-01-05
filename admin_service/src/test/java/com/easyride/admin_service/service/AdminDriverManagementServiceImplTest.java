package com.easyride.admin_service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easyride.admin_service.dto.DriverApplicationDto;
import com.easyride.admin_service.dto.DriverApplicationEventDto_Consumed;
import com.easyride.admin_service.dto.DriverApplicationReviewedEvent;
import com.easyride.admin_service.model.DriverApplication;
import com.easyride.admin_service.model.DriverApplicationStatus;
import com.easyride.admin_service.repository.DriverApplicationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDriverManagementServiceImplTest {

    @Mock
    private DriverApplicationMapper applicationMapper;

    @Mock
    private DriverVerificationService driverVerificationService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private AdminDriverManagementServiceImpl service;

    @Test
    void processNewDriverApplication_Success() {
        DriverApplicationEventDto_Consumed event = new DriverApplicationEventDto_Consumed();
        event.setDriverId(1L);
        event.setUsername("driver1");
        event.setDriverLicenseNumber("L12345");
        event.setApplicationTime(LocalDateTime.now());

        when(applicationMapper.selectById(1L)).thenReturn(null);

        service.processNewDriverApplication(event);

        verify(applicationMapper).insert(any(DriverApplication.class));
    }

    @Test
    void processNewDriverApplication_Duplicate() {
        DriverApplicationEventDto_Consumed event = new DriverApplicationEventDto_Consumed();
        event.setDriverId(1L);

        when(applicationMapper.selectById(1L)).thenReturn(new DriverApplication());

        service.processNewDriverApplication(event);

        verify(applicationMapper, never()).insert(any(DriverApplication.class));
    }

    @Test
    void getPendingDriverApplications_Success() {
        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        Page<DriverApplication> mybatisPage = new Page<>();
        mybatisPage.setRecords(Collections.singletonList(app));
        mybatisPage.setTotal(1);

        when(applicationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mybatisPage);

        org.springframework.data.domain.Page<DriverApplicationDto> result = service.getPendingDriverApplications(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("driver1", result.getContent().get(0).getUsername());
    }

    @Test
    void approveDriverApplication_Success() {
        // Set topic name
        ReflectionTestUtils.setField(service, "driverReviewTopic", "test-topic");

        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        app.setStatus(DriverApplicationStatus.PENDING_REVIEW);

        when(applicationMapper.selectById(1L)).thenReturn(app);

        service.approveDriverApplication(1L, 100L, "Looks good");

        assertEquals(DriverApplicationStatus.APPROVED, app.getStatus());
        assertEquals(100L, app.getReviewedByAdminId());
        verify(applicationMapper).updateById(app);
        verify(rocketMQTemplate).convertAndSend(eq("test-topic"), any(DriverApplicationReviewedEvent.class));
    }

    @Test
    void approveDriverApplication_FailIfNotPending() {
        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        app.setStatus(DriverApplicationStatus.APPROVED);

        when(applicationMapper.selectById(1L)).thenReturn(app);

        assertThrows(IllegalStateException.class, () -> service.approveDriverApplication(1L, 100L, "Notes"));
    }

    @Test
    void rejectDriverApplication_Success() {
        ReflectionTestUtils.setField(service, "driverReviewTopic", "test-topic");

        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        app.setStatus(DriverApplicationStatus.PENDING_REVIEW);

        when(applicationMapper.selectById(1L)).thenReturn(app);

        service.rejectDriverApplication(1L, 100L, "Bad license", "Rejection notes");

        assertEquals(DriverApplicationStatus.REJECTED, app.getStatus());
        verify(applicationMapper).updateById(app);
        verify(rocketMQTemplate).convertAndSend(eq("test-topic"), any(DriverApplicationReviewedEvent.class));
    }
}
