package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.FocusAreaDto;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.entity.Category;
import com.testlyflow.entity.Question;
import com.testlyflow.repository.PrepLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        PrepLinkRepository prepLinkRepository = mock(PrepLinkRepository.class);
        when(prepLinkRepository.findByCategoryIdOrderBySortOrderAsc(anyLong())).thenReturn(List.of());
        TestlyProperties properties = new TestlyProperties();
        feedbackService = new FeedbackService(properties, prepLinkRepository);
        feedbackService.loadMessages();
    }

    private AttemptAnswer answer(long categoryId, String categoryName, boolean correct) {
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        Question question = new Question();
        question.setCategory(category);
        AttemptAnswer answer = new AttemptAnswer();
        answer.setQuestion(question);
        answer.setCorrect(correct);
        return answer;
    }

    @Test
    void belowFiftyPercentIsGrowthTier() {
        assertEquals(FeedbackService.ResultTier.GROWTH,
                feedbackService.buildFeedback(List.of(), new BigDecimal("49.00")).tier());
    }

    @Test
    void fiftyPercentIsSolidTier() {
        assertEquals(FeedbackService.ResultTier.SOLID,
                feedbackService.buildFeedback(List.of(), new BigDecimal("50.00")).tier());
    }

    @Test
    void seventyFourPercentIsSolidTier() {
        assertEquals(FeedbackService.ResultTier.SOLID,
                feedbackService.buildFeedback(List.of(), new BigDecimal("74.00")).tier());
    }

    @Test
    void seventyFivePercentIsStrongTier() {
        assertEquals(FeedbackService.ResultTier.STRONG,
                feedbackService.buildFeedback(List.of(), new BigDecimal("75.00")).tier());
    }

    @Test
    void eightyNinePercentIsStrongTier() {
        assertEquals(FeedbackService.ResultTier.STRONG,
                feedbackService.buildFeedback(List.of(), new BigDecimal("89.00")).tier());
    }

    @Test
    void ninetyPercentIsExemplaryTier() {
        assertEquals(FeedbackService.ResultTier.EXEMPLARY,
                feedbackService.buildFeedback(List.of(), new BigDecimal("90.00")).tier());
    }

    @Test
    void focusAreasContainOnlyCategoriesBelowAverageSortedAscendingCappedAtThree() {
        List<AttemptAnswer> answers = List.of(
                answer(1, "Weakest", false), answer(1, "Weakest", false), answer(1, "Weakest", false), answer(1, "Weakest", true), // 25%
                answer(2, "Mid", false), answer(2, "Mid", true), // 50%
                answer(3, "Strong", true), answer(3, "Strong", true), // 100%
                answer(4, "AlsoWeak", false), answer(4, "AlsoWeak", true) // 50%
        );

        FeedbackService.Feedback feedback = feedbackService.buildFeedback(answers, new BigDecimal("60.00"));
        List<FocusAreaDto> focusAreas = feedback.focusAreas();

        assertTrue(focusAreas.size() <= 3);
        assertEquals("Weakest", focusAreas.get(0).categoryName());
        for (int i = 1; i < focusAreas.size(); i++) {
            assertTrue(focusAreas.get(i - 1).correctRate().doubleValue() <= focusAreas.get(i).correctRate().doubleValue());
        }
        assertTrue(focusAreas.stream().noneMatch(a -> a.categoryName().equals("Strong")));
    }
}
