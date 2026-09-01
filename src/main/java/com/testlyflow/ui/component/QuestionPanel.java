package com.testlyflow.ui.component;

import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.dto.QuestionOptionDto;
import com.testlyflow.ui.support.NativeUi;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;

import java.util.function.Consumer;

public class QuestionPanel extends Div {

    public QuestionPanel(AttemptQuestionDto question, String selectedOption,
                         boolean isFirst, boolean isLast,
                         Consumer<String> onSelect,
                         Runnable onPrev, Runnable onNext, Runnable onFinish) {
        addClassName("question-panel");

        Span legend = new Span(question.text());
        legend.setId("question-text");
        legend.addClassName("question-text");
        add(legend);

        Div options = new Div();
        options.addClassName("option-list");
        options.getElement().setAttribute("role", "group");
        options.getElement().setAttribute("aria-labelledby", "question-text");
        for (QuestionOptionDto option : question.options()) {
            boolean selected = option.letter().equals(selectedOption);
            NativeButton card = new NativeButton();
            card.addClassName("option-card");
            if (selected) {
                card.addClassName("option-card-selected");
            }
            card.getElement().setAttribute("aria-pressed", selected ? "true" : "false");
            card.addClickListener(e -> onSelect.accept(option.letter()));

            Span letter = new Span(option.letter());
            letter.addClassName("option-letter");
            Span text = new Span(option.text());
            text.addClassName("option-text");
            card.add(letter, text);
            options.add(card);
        }
        add(options);

        Div nav = new Div();
        nav.addClassName("attempt-nav-buttons");
        NativeButton prev = NativeUi.button("← Назад", e -> onPrev.run(), "btn", "btn-secondary");
        prev.setEnabled(!isFirst);
        NativeButton next = NativeUi.button("Далее →", e -> onNext.run(), "btn", "btn-secondary");
        next.setEnabled(!isLast);
        NativeButton finish = NativeUi.button("Завершить тест", e -> onFinish.run(), "btn", "btn-finish");
        nav.add(prev, next, finish);
        add(nav);
    }
}
