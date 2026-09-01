package com.testlyflow.ui.component;

import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.ui.support.NativeUi;
import com.testlyflow.ui.support.RussianPlurals;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FinishDialog extends Div {

    public FinishDialog(List<AttemptQuestionDto> questions,
                        Map<Long, String> answers,
                        boolean submitting,
                        String submitError,
                        Runnable onCancelToFirstUnanswered,
                        Runnable onCancelContinue,
                        Runnable onSubmit) {
        addClassName("modal-backdrop");
        getElement().setAttribute("role", "dialog");
        getElement().setAttribute("aria-modal", "true");
        getElement().setAttribute("aria-labelledby", "confirm-title");

        Div modal = new Div();
        modal.addClassName("modal");
        H3 title = new H3("Завершить тест?");
        title.setId("confirm-title");
        modal.add(title);

        List<AttemptQuestionDto> unanswered = questions.stream()
                .filter(q -> {
                    String selected = answers.get(q.questionId());
                    return selected == null || selected.isBlank();
                })
                .toList();

        Div actions = new Div();
        actions.addClassName("modal-actions");

        if (!unanswered.isEmpty()) {
            String numbers = unanswered.stream()
                    .map(q -> String.valueOf(q.displayNumber()))
                    .collect(Collectors.joining(", "));
            modal.add(new Paragraph(
                    "Без ответа осталось " + unanswered.size() + " "
                            + RussianPlurals.questions(unanswered.size()) + ": " + numbers + "."));
            actions.add(NativeUi.button("Вернуться к первому пропущенному",
                    e -> onCancelToFirstUnanswered.run(), "btn", "btn-secondary"));
            var finishAnyway = NativeUi.button(submitting ? "Завершаем…" : "Всё равно завершить",
                    e -> onSubmit.run(), "btn", "btn-finish");
            finishAnyway.setEnabled(!submitting);
            actions.add(finishAnyway);
        } else {
            actions.add(NativeUi.button("Продолжить", e -> onCancelContinue.run(), "btn", "btn-secondary"));
            var finish = NativeUi.button(submitting ? "Завершаем…" : "Завершить",
                    e -> onSubmit.run(), "btn", "btn-finish");
            finish.setEnabled(!submitting);
            actions.add(finish);
        }
        modal.add(actions);
        if (submitError != null && !submitError.isBlank()) {
            Paragraph error = new Paragraph(submitError);
            error.addClassName("field-error");
            modal.add(error);
        }
        add(modal);
    }
}
