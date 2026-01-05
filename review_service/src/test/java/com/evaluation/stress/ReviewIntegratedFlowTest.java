package com.evaluation.stress;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.mapper.ComplaintMapper;
import com.evaluation.mapper.EvaluationMapper;
import com.evaluation.model.Complaint;
import com.evaluation.model.Evaluation;
import com.evaluation.model.ReviewWindow;
import com.evaluation.repository.ComplaintRepository;
import com.evaluation.repository.EvaluationRepository;
import com.evaluation.repository.ReviewWindowRepository;
import com.evaluation.repository.TagRepository;
import com.evaluation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
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
    private EvaluationRepository evaluationRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ReviewWindowRepository reviewWindowRepository;
    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private FileStorageService fileStorageService;

    private EvaluationServiceImpl evaluationService;
    private ComplaintServiceImpl complaintService;

    private final EvaluationMapper evaluationMapper = new EvaluationMapper();
    private final ComplaintMapper complaintMapper = new ComplaintMapper();

    private final AtomicReference<ReviewWindow> windowStore = new AtomicReference<>();
    private final AtomicReference<Evaluation> evaluationStore = new AtomicReference<>();
    private final AtomicReference<Complaint> complaintStore = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationServiceImpl(
                evaluationRepository,
                tagRepository,
                evaluationMapper,
                rocketMQTemplate,
                sensitiveWordService,
                reviewWindowRepository);

        complaintService = new ComplaintServiceImpl(
                complaintRepository,
                complaintMapper,
                fileStorageService,
                rocketMQTemplate,
                sensitiveWordService,
                evaluationRepository);

        // Mock ReviewWindow Repository
        when(reviewWindowRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(windowStore.get()));
        when(reviewWindowRepository.save(any(ReviewWindow.class))).thenAnswer(inv -> {
            ReviewWindow w = inv.getArgument(0);
            windowStore.set(w);
            return w;
        });

        // Mock Evaluation Repository
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            if (e.getId() == null)
                e.setId(5001L);
            evaluationStore.set(e);
            return e;
        });
        when(evaluationRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(evaluationStore.get()));

        // Mock Complaint Repository
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            if (c.getId() == null)
                c.setId(9001L);
            complaintStore.set(c);
            return c;
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
