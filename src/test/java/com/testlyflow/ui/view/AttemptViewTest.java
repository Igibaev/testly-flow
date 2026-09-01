package com.testlyflow.ui.view;

import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.testlyflow.dto.AnswerUpdateRequest;
import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.dto.AttemptStateDto;
import com.testlyflow.dto.QuestionOptionDto;
import com.testlyflow.service.AttemptService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttemptViewTest {

    private AttemptService attemptService;

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
        attemptService = mock(AttemptService.class);
        when(attemptService.getAttemptState(1L)).thenReturn(sampleState());
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void showsFirstQuestionAndSavesOnSelect() {
        AttemptView view = new AttemptView(attemptService);
        UI.getCurrent().add(view);
        view.load("1");

        LocatorJ._assert(Paragraph.class, 1, spec -> spec.withText("Вопрос 1 из 2"));
        LocatorJ._click(optionCard("вариант А"));

        ArgumentCaptor<AnswerUpdateRequest> captor = ArgumentCaptor.forClass(AnswerUpdateRequest.class);
        verify(attemptService, atLeastOnce()).updateAnswer(eq(1L), eq(10L), captor.capture());
        assertEquals("А", captor.getValue().selectedOption());
    }

    @Test
    void secondClickClearsTheAnswer() {
        AttemptView view = new AttemptView(attemptService);
        UI.getCurrent().add(view);
        view.load("1");

        LocatorJ._click(optionCard("вариант А"));
        LocatorJ._click(optionCard("вариант А"));

        ArgumentCaptor<AnswerUpdateRequest> captor = ArgumentCaptor.forClass(AnswerUpdateRequest.class);
        verify(attemptService, atLeastOnce()).updateAnswer(eq(1L), eq(10L), captor.capture());
        assertNull(captor.getValue().selectedOption());
    }

    @Test
    void navigatorAndNextChangeTheQuestion() {
        AttemptView view = new AttemptView(attemptService);
        UI.getCurrent().add(view);
        view.load("1");

        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Далее →")));
        LocatorJ._assert(Paragraph.class, 1, spec -> spec.withText("Вопрос 2 из 2"));

        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withClasses("nav-cell")
                .withText("1")));
        LocatorJ._assert(Paragraph.class, 1, spec -> spec.withText("Вопрос 1 из 2"));
    }

    @Test
    void finishDialogListsUnansweredNumbers() {
        AttemptView view = new AttemptView(attemptService);
        UI.getCurrent().add(view);
        view.load("1");

        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Завершить тест")));
        LocatorJ._assert(Paragraph.class, 1, spec -> spec.withPredicate(
                p -> p.getText() != null && p.getText().contains("Без ответа осталось") && p.getText().contains("1, 2")));
    }

    private AttemptStateDto sampleState() {
        AttemptQuestionDto q1 = new AttemptQuestionDto(10L, 1, 1L, "Блок", "Текст 1?",
                List.of(new QuestionOptionDto("А", "вариант А"), new QuestionOptionDto("Б", "вариант Б")));
        AttemptQuestionDto q2 = new AttemptQuestionDto(11L, 2, 1L, "Блок", "Текст 2?",
                List.of(new QuestionOptionDto("А", "вариант А"), new QuestionOptionDto("Б", "вариант Б")));
        return new AttemptStateDto(1L, OffsetDateTime.now(), List.of(q1, q2), 2, 1, List.of());
    }

    private NativeButton optionCard(String optionText) {
        return LocatorJ._get(NativeButton.class, spec -> spec.withClasses("option-card")
                .withPredicate(button -> button.getChildren()
                        .anyMatch(child -> child instanceof com.vaadin.flow.component.html.Span span
                                && optionText.equals(span.getText()))));
    }
}
