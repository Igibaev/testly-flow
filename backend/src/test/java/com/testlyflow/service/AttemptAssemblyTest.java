package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.StartAttemptRequest;
import com.testlyflow.dto.StartAttemptResponse;
import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.entity.Category;
import com.testlyflow.entity.Question;
import com.testlyflow.entity.QuestionOption;
import com.testlyflow.exception.ConflictException;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import com.testlyflow.repository.CategoryRepository;
import com.testlyflow.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AttemptAssemblyTest {

    private AttemptRepository attemptRepository;
    private AttemptAnswerRepository attemptAnswerRepository;
    private QuestionRepository questionRepository;
    private CategoryRepository categoryRepository;
    private TestlyProperties properties;
    private AttemptService attemptService;

    @BeforeEach
    void setUp() {
        attemptRepository = mock(AttemptRepository.class);
        attemptAnswerRepository = mock(AttemptAnswerRepository.class);
        questionRepository = mock(QuestionRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        FeedbackService feedbackService = mock(FeedbackService.class);
        properties = new TestlyProperties();
        properties.getSampling().setQuestionsPerCategoryMin(2);
        properties.getSampling().setQuestionsPerCategoryMax(3);
        properties.getSampling().setShuffleQuestions(false);

        attemptService = new AttemptService(attemptRepository, attemptAnswerRepository, questionRepository,
                categoryRepository, feedbackService, properties);

        when(attemptRepository.save(any(Attempt.class))).thenAnswer(inv -> {
            Attempt a = inv.getArgument(0);
            a.setId(1L);
            a.setStartedAt(OffsetDateTime.now());
            return a;
        });
        when(attemptAnswerRepository.save(any(AttemptAnswer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Category category(long id, String name, Integer min, Integer max) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setQuestionsMin(min);
        c.setQuestionsMax(max);
        c.setSortOrder((int) id);
        return c;
    }

    private Question question(long id, Category category) {
        Question q = new Question();
        q.setId(id);
        q.setCategory(category);
        q.setNumber((int) id);
        q.setText("Q" + id);
        q.setCorrectOption("А");
        QuestionOption a = new QuestionOption();
        a.setOptionLetter("А");
        a.setText("a");
        q.getOptions().add(a);
        return q;
    }

    @Test
    void throwsConflictWhenNoCategoriesHaveQuestions() {
        when(categoryRepository.findAllWithQuestionsOrdered()).thenReturn(List.of());

        assertThrows(ConflictException.class,
                () -> attemptService.startAttempt(new StartAttemptRequest("Ivan", "Petrov", "team"), "1.2.3.4", "UA"));
    }

    @Test
    void takesAllQuestionsWhenCategoryHasFewerThanMinimum() {
        Category cat = category(1, "Cat A", null, null);
        when(categoryRepository.findAllWithQuestionsOrdered()).thenReturn(List.of(cat));

        // category has only 1 question available, min configured is 2
        when(questionRepository.sampleRandomIdsByCategory(eq(1L), anyInt())).thenReturn(List.of(10L));
        Question q = question(10, cat);
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q));

        StartAttemptResponse response = attemptService.startAttempt(
                new StartAttemptRequest("Ivan", "Petrov", "team"), "1.2.3.4", "UA");

        assertEquals(1, response.totalQuestions());
        assertEquals(1, response.categoriesCount());
    }

    @Test
    void skipsCategoriesWithoutQuestionsAndDoesNotDuplicateQuestions() {
        Category cat1 = category(1, "Cat A", 2, 2);
        Category cat2 = category(2, "Cat B", 1, 1);
        when(categoryRepository.findAllWithQuestionsOrdered()).thenReturn(List.of(cat1, cat2));

        when(questionRepository.sampleRandomIdsByCategory(eq(1L), eq(2))).thenReturn(List.of(11L, 12L));
        when(questionRepository.sampleRandomIdsByCategory(eq(2L), eq(1))).thenReturn(List.of(21L));

        Question q11 = question(11, cat1);
        Question q12 = question(12, cat1);
        Question q21 = question(21, cat2);
        when(questionRepository.findAllById(anyList())).thenReturn(List.of(q11, q12, q21));

        StartAttemptResponse response = attemptService.startAttempt(
                new StartAttemptRequest("Ivan", "Petrov", "team"), "1.2.3.4", "UA");

        assertEquals(3, response.totalQuestions());
        assertEquals(2, response.categoriesCount());
        List<Long> ids = response.questions().stream().map(q -> q.questionId()).toList();
        assertEquals(ids.size(), ids.stream().distinct().count(), "questions must not repeat within an attempt");
    }

    @Test
    void picksSampleSizeWithinConfiguredRangePerCategory() {
        Category cat = category(1, "Cat A", null, null); // falls back to global 2..3
        when(categoryRepository.findAllWithQuestionsOrdered()).thenReturn(List.of(cat));

        when(questionRepository.sampleRandomIdsByCategory(eq(1L), anyInt())).thenAnswer(inv -> {
            int n = inv.getArgument(1);
            assertTrue(n >= 2 && n <= 3, "sampled n must be within [min, max]");
            return List.of(1L, 2L, 3L).subList(0, n);
        });
        when(questionRepository.findAllById(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return ids.stream().map(id -> question(id, cat)).toList();
        });

        StartAttemptResponse response = attemptService.startAttempt(
                new StartAttemptRequest("Ivan", "Petrov", "team"), "1.2.3.4", "UA");

        assertTrue(response.totalQuestions() >= 2 && response.totalQuestions() <= 3);
    }
}
