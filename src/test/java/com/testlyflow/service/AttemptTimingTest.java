package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.*;
import com.testlyflow.entity.*;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import com.testlyflow.repository.CategoryRepository;
import com.testlyflow.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AttemptTimingTest {

    private AttemptRepository attemptRepository;
    private AttemptAnswerRepository attemptAnswerRepository;
    private FeedbackService feedbackService;
    private TestlyProperties properties;
    private AttemptService attemptService;

    private Attempt attempt;
    private Question question;
    private AttemptAnswer answer;

    @BeforeEach
    void setUp() {
        attemptRepository = mock(AttemptRepository.class);
        attemptAnswerRepository = mock(AttemptAnswerRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        feedbackService = mock(FeedbackService.class);
        properties = new TestlyProperties();
        properties.getTiming().setMaxTimeSpentMs(60_000L);

        attemptService = new AttemptService(attemptRepository, attemptAnswerRepository, questionRepository,
                categoryRepository, feedbackService, properties);

        Category category = new Category();
        category.setId(1L);
        category.setName("Cat");

        question = new Question();
        question.setId(10L);
        question.setCategory(category);
        question.setNumber(1);
        question.setText("Q?");
        question.setCorrectOption("А");
        QuestionOption opt = new QuestionOption();
        opt.setOptionLetter("А");
        opt.setText("a");
        question.getOptions().add(opt);

        attempt = new Attempt();
        attempt.setId(5L);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(OffsetDateTime.now().minusMinutes(10));

        answer = new AttemptAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(question);
        answer.setDisplayNumber(1);

        when(attemptRepository.findById(5L)).thenReturn(Optional.of(attempt));
        when(attemptAnswerRepository.findByAttemptIdAndQuestionId(5L, 10L)).thenReturn(Optional.of(answer));
        when(attemptAnswerRepository.save(any(AttemptAnswer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void overwritesAccumulatedTimeRatherThanSumming() {
        attemptService.updateAnswer(5L, 10L, new AnswerUpdateRequest("А", 5_000L));
        assertEquals(5_000L, answer.getTimeSpentMs());

        attemptService.updateAnswer(5L, 10L, new AnswerUpdateRequest("А", 9_000L));
        assertEquals(9_000L, answer.getTimeSpentMs(), "second submission must overwrite, not add to, the accumulated value");
    }

    @Test
    void negativeTimeIsClampedToZero() {
        attemptService.updateAnswer(5L, 10L, new AnswerUpdateRequest(null, -500L));
        assertEquals(0L, answer.getTimeSpentMs());
    }

    @Test
    void timeAboveCeilingIsClampedToCeiling() {
        attemptService.updateAnswer(5L, 10L, new AnswerUpdateRequest(null, 999_999L));
        assertEquals(60_000L, answer.getTimeSpentMs());
    }

    @Test
    void flagsAttemptAsSuspiciousWhenTimingsExceedAttemptDurationByMoreThanTenPercent() {
        // attempt lasted 10 minutes (600_000 ms); reported time spent grossly exceeds that
        answer.setTimeSpentMs(50_000_000L);
        when(attemptAnswerRepository.findByAttemptIdOrderByDisplayNumberAsc(5L)).thenReturn(List.of(answer));
        when(feedbackService.buildFeedback(anyList(), any(BigDecimal.class)))
                .thenReturn(new FeedbackService.Feedback(FeedbackService.ResultTier.GROWTH, "h", "m", List.of()));
        when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

        attemptService.submitAttempt(5L, new SubmitAttemptRequest(null, null));

        assertTrue(attempt.isTimingSuspicious());
    }

    @Test
    void doesNotFlagAttemptWhenTimingsAreWithinTenPercentOfDuration() {
        answer.setTimeSpentMs(500_000L); // close to the 600_000ms attempt duration
        when(attemptAnswerRepository.findByAttemptIdOrderByDisplayNumberAsc(5L)).thenReturn(List.of(answer));
        when(feedbackService.buildFeedback(anyList(), any(BigDecimal.class)))
                .thenReturn(new FeedbackService.Feedback(FeedbackService.ResultTier.GROWTH, "h", "m", List.of()));
        when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));

        attemptService.submitAttempt(5L, new SubmitAttemptRequest(null, null));

        assertFalse(attempt.isTimingSuspicious());
    }
}
