package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.DriverApplicationDto;
import com.easyride.admin_service.dto.DriverApplicationEventDto_Consumed;
import com.easyride.admin_service.dto.DriverApplicationReviewedEvent;
import com.easyride.admin_service.model.DriverApplication;
import com.easyride.admin_service.model.DriverApplicationStatus;
import com.easyride.admin_service.repository.DriverApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDriverManagementServiceImplTest {

    @Mock
    private DriverApplicationRepository applicationRepository;

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

        when(applicationRepository.existsById(1L)).thenReturn(false);

        service.processNewDriverApplication(event);

        verify(applicationRepository).save(any(DriverApplication.class));
    }

    @Test
    void processNewDriverApplication_Duplicate() {
        DriverApplicationEventDto_Consumed event = new DriverApplicationEventDto_Consumed();
        event.setDriverId(1L);

        when(applicationRepository.existsById(1L)).thenReturn(true);

        service.processNewDriverApplication(event);

        verify(applicationRepository, never()).save(any(DriverApplication.class));
    }

    @Test
    void getPendingDriverApplications_Success() {
        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        Page<DriverApplication> page = new PageImpl<>(Collections.singletonList(app));

        when(applicationRepository.findByStatus(eq(DriverApplicationStatus.PENDING_REVIEW), any(PageRequest.class)))
                .thenReturn(page);

        Page<DriverApplicationDto> result = service.getPendingDriverApplications(0, 10);

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

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        service.approveDriverApplication(1L, 100L, "Looks good");

        assertEquals(DriverApplicationStatus.APPROVED, app.getStatus());
        assertEquals(100L, app.getReviewedByAdminId());
        verify(applicationRepository).save(app);
        verify(rocketMQTemplate).convertAndSend(eq("test-topic"), any(DriverApplicationReviewedEvent.class));
    }

    @Test
    void approveDriverApplication_FailIfNotPending() {
        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        app.setStatus(DriverApplicationStatus.APPROVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThrows(IllegalStateException.class, () -> service.approveDriverApplication(1L, 100L, "Notes"));
    }

    @Test
    void rejectDriverApplication_Success() {
        ReflectionTestUtils.setField(service, "driverReviewTopic", "test-topic");

        DriverApplication app = new DriverApplication(1L, "driver1", "L12345", LocalDateTime.now());
        app.setStatus(DriverApplicationStatus.PENDING_REVIEW);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        service.rejectDriverApplication(1L, 100L, "Bad license", "Rejection notes");

        assertEquals(DriverApplicationStatus.REJECTED, app.getStatus());
        verify(applicationRepository).save(app);
        verify(rocketMQTemplate).convertAndSend(eq("test-topic"), any(DriverApplicationReviewedEvent.class));
    }
}
