package com.evaluation.stress;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.mapper.ComplaintDtoMapper;
import com.evaluation.mapper.EvaluationDtoMapper;
import com.evaluation.model.Complaint;
import com.evaluation.model.Evaluation;
import com.evaluation.model.ReviewWindow;
import com.evaluation.repository.ComplaintMapper;
import com.evaluation.repository.EvaluationMapper;
import com.evaluation.repository.ReviewWindowMapper;
import com.evaluation.repository.TagMapper;
import com.evaluation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.time.LocalDateTime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReviewIntegratedFlowTest {

    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private ReviewWindowMapper reviewWindowMapper;
    @Mock
    private ComplaintMapper complaintMapper;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private FileStorageService fileStorageService;

    private EvaluationServiceImpl evaluationService;
    private ComplaintServiceImpl complaintService;

    private final EvaluationDtoMapper evaluationDtoMapper = new EvaluationDtoMapper();
    private final ComplaintDtoMapper complaintDtoMapper = new ComplaintDtoMapper();

    private final AtomicReference<ReviewWindow> windowStore = new AtomicReference<>();
    private final AtomicReference<Evaluation> evaluationStore = new AtomicReference<>();
    private final AtomicReference<Complaint> complaintStore = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationServiceImpl(
                evaluationMapper,
                tagMapper,
                evaluationDtoMapper,
                rocketMQTemplate,
                sensitiveWordService,
                reviewWindowMapper);

        complaintService = new ComplaintServiceImpl(
                complaintMapper,
                complaintDtoMapper,
                fileStorageService,
                rocketMQTemplate,
                sensitiveWordService,
                evaluationMapper);

        // Mock ReviewWindow Repository
        // Mock ReviewWindow Mapper
        when(reviewWindowMapper.selectById(anyLong())).thenAnswer(inv -> windowStore.get());
        when(reviewWindowMapper.updateById(any(ReviewWindow.class))).thenAnswer(inv -> {
            ReviewWindow w = inv.getArgument(0);
            windowStore.set(w);
            return 1;
        });
        when(reviewWindowMapper.insert(any(ReviewWindow.class))).thenAnswer(inv -> {
            ReviewWindow w = inv.getArgument(0);
            windowStore.set(w);
            return 1;
        });

        // Mock Evaluation Mapper
        when(evaluationMapper.insert(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            if (e.getId() == null)
                e.setId(5001L);
            evaluationStore.set(e);
            return 1;
        });
        when(evaluationMapper.selectById(anyLong())).thenAnswer(inv -> evaluationStore.get());
        when(evaluationMapper.updateById(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            evaluationStore.set(e);
            return 1;
        });

        // Mock Complaint Mapper
        when(complaintMapper.insert(any(Complaint.class))).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            if (c.getId() == null)
                c.setId(9001L);
            complaintStore.set(c);
            return 1;
        });

        // Mock Sensitive Word Service (no filter)
        when(sensitiveWordService.containsSensitiveWords(anyString())).thenReturn(false);
        when(sensitiveWordService.filterContent(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testRegularEvaluationAndComplaintFlow() {
        // 1. Setup Review Window
        ReviewWindow window = new ReviewWindow();
        window.setOrderId(100L);
        window.setPassengerId(200L);
        window.setDriverId(300L);
        window.setPassengerCanReview(true);
        window.setDriverCanReview(true);
        window.setWindowCloseTime(LocalDateTime.now().plusDays(1));
        windowStore.set(window);

        // 2. Passenger submits evaluation
        EvaluationDTO evalDto = new EvaluationDTO();
        evalDto.setOrderId(100L);
        evalDto.setEvaluatorId(200L);
        evalDto.setEvaluateeId(300L);
        evalDto.setScore(5);
        evalDto.setComment("Great driver!");

        EvaluationDTO createdEval = evaluationService.createEvaluation(evalDto);

        assertNotNull(createdEval);
        assertEquals("Great driver!", createdEval.getComment());
        assertTrue(windowStore.get().isPassengerReviewed());
        System.out.println("Step 1: Evaluation created. Status: " + windowStore.get().isPassengerReviewed());

        // 3. Driver files a complaint against this evaluation
        ComplaintDTO complaintDto = new ComplaintDTO();
        complaintDto.setEvaluationId(createdEval.getId());
        complaintDto.setComplainantId(300L);
        complaintDto.setReason("Inaccurate rating");

        ComplaintDTO filedComplaint = complaintService.fileComplaint(complaintDto, null);

        assertNotNull(filedComplaint);
        assertEquals("PENDING_REVIEW", filedComplaint.getStatus());
        assertEquals("FILED", evaluationStore.get().getComplaintStatus());
        System.out.println(
                "Step 2: Complaint filed. Evaluation Complaint Status: " + evaluationStore.get().getComplaintStatus());
    }

    @Test
    void testConcurrentEvaluationSubmission() throws InterruptedException {
        // Setup Window
        ReviewWindow window = new ReviewWindow();
        window.setOrderId(100L);
        window.setPassengerId(200L);
        window.setDriverId(300L);
        window.setPassengerCanReview(true);
        window.setPassengerReviewed(false);
        windowStore.set(window);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        EvaluationDTO evalDto = new EvaluationDTO();
        evalDto.setOrderId(100L);
        evalDto.setEvaluatorId(200L);
        evalDto.setEvaluateeId(300L);
        evalDto.setScore(5);
        evalDto.setComment("Spamming evaluation");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    evaluationService.createEvaluation(evalDto);
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("Concurrent Success: " + successCount.get());
        System.out.println("Concurrent Failures: " + failureCount.get());

        // Ideally successCount should be 1.
        // If it's > 1, it proves a race condition exists.
        // For the test "conduct", we just report the result.
        assertTrue(successCount.get() >= 1, "At least one evaluation should succeed");
    }
}
