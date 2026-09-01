package com.testlyflow.ui.view;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.dto.AttemptStateDto;
import com.testlyflow.dto.FocusAreaDto;
import com.testlyflow.dto.PublicAnswerDetailDto;
import com.testlyflow.dto.QuestionOptionDto;
import com.testlyflow.dto.SubmitAttemptResponse;
import com.testlyflow.service.AttemptService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttemptUiSecurityTest {

    private AttemptService attemptService;

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
        attemptService = mock(AttemptService.class);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void attemptViewDoesNotMarkTheCorrectOption() {
        when(attemptService.getAttemptState(1L)).thenReturn(stateWithCorrectHiddenInOptions());
        AttemptView view = new AttemptView(attemptService);
        UI.getCurrent().add(view);
        view.load("1");
        assertNoCorrectnessLeak(view, "В");
    }

    @Test
    void resultViewDoesNotRevealTheCorrectOption() {
        SubmitAttemptResponse result = new SubmitAttemptResponse(
                1L, 0, 1, BigDecimal.ZERO, "GROWTH", "Заголовок", "Сообщение",
                List.of(new FocusAreaDto(1L, "Блок", BigDecimal.ZERO, 1, List.of())),
                List.of(new PublicAnswerDetailDto(10L, 1, "Текст?", 1L, "Блок", "А", false, 1000)));

        ResultView view = new ResultView();
        UI.getCurrent().add(view);
        view.show(result);
        String tree = flatten(view).toLowerCase(Locale.ROOT);
        assertFalse(tree.contains("правильн"), tree);
        assertFalse(tree.contains("correctoption"), tree);
        assertFalse(tree.contains("верный вариант"), tree);
    }

    private AttemptStateDto stateWithCorrectHiddenInOptions() {
        AttemptQuestionDto q = new AttemptQuestionDto(10L, 1, 1L, "Блок", "Текст?",
                List.of(new QuestionOptionDto("А", "первый"),
                        new QuestionOptionDto("Б", "второй"),
                        new QuestionOptionDto("В", "третий")));
        return new AttemptStateDto(1L, OffsetDateTime.now(), List.of(q), 1, 1, List.of());
    }

    private static void assertNoCorrectnessLeak(Component root, String correctLetter) {
        String tree = flatten(root).toLowerCase(Locale.ROOT);
        assertFalse(tree.contains("правильн"), tree);
        assertFalse(tree.contains("correctoption"), tree);
        assertFalse(tree.contains("iscorrect"), tree);
        assertFalse(tree.contains("верный: " + correctLetter.toLowerCase(Locale.ROOT)), tree);
    }

    private static String flatten(Component root) {
        StringBuilder sb = new StringBuilder();
        walk(root, sb);
        return sb.toString();
    }

    private static void walk(Component component, StringBuilder sb) {
        if (component.getElement() != null) {
            sb.append(component.getElement().getText()).append(' ');
            component.getElement().getAttributeNames()
                    .forEach(name -> sb.append(name).append('=')
                            .append(component.getElement().getAttribute(name)).append(' '));
        }
        component.getChildren().forEach(child -> walk(child, sb));
    }
}
