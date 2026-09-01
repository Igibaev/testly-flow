package com.testlyflow.ui.view;

import com.testlyflow.dto.FocusAreaDto;
import com.testlyflow.dto.PrepLinkDto;
import com.testlyflow.dto.PublicAnswerDetailDto;
import com.testlyflow.dto.SubmitAttemptResponse;
import com.testlyflow.ui.MainLayout;
import com.testlyflow.ui.support.NativeUi;
import com.testlyflow.ui.support.RussianPlurals;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.List;
import java.util.Map;

@Route(value = "attempt/:attemptId/result", layout = MainLayout.class)
@PageTitle("Результат теста")
public class ResultView extends Div implements BeforeEnterObserver {

    private static final Map<String, String> TIER_LABELS = Map.of(
            "GROWTH", "Точка роста",
            "SOLID", "Уверенная база",
            "STRONG", "Сильный результат",
            "EXEMPLARY", "Отличный результат");

    public ResultView() {
        addClassName("result-page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String attemptId = event.getRouteParameters().get("attemptId").orElse("");
        Object stored = VaadinSession.getCurrent() == null ? null
                : VaadinSession.getCurrent().getAttribute(AttemptView.RESULT_SESSION_PREFIX + attemptId);
        show(stored instanceof SubmitAttemptResponse result ? result : null);
    }

    public void show(SubmitAttemptResponse result) {
        removeAll();
        if (result == null) {
            Div empty = new Div();
            empty.addClassName("state-empty");
            empty.add(new Paragraph("Результат недоступен — возможно, страница была перезагружена."));
            empty.add(NativeUi.button("На главную", e -> getUI().ifPresent(ui -> ui.navigate(HomeView.class)),
                    "btn", "btn-primary"));
            add(empty);
            return;
        }
        render(result);
    }

    private void render(SubmitAttemptResponse result) {
        Div hero = new Div();
        hero.addClassName("result-hero");
        Paragraph tier = new Paragraph(TIER_LABELS.getOrDefault(result.resultTier(), result.resultTier()));
        tier.addClassName("result-tier-label");
        hero.add(tier, new H1(result.headline()));
        Paragraph score = new Paragraph();
        score.addClassName("result-score");
        score.add(new Span(String.valueOf(result.correctCount())));
        Span of = new Span(" из " + result.totalQuestions());
        score.add(of);
        Paragraph message = new Paragraph(result.message());
        message.addClassName("result-message");
        hero.add(score, message);
        add(hero);

        if (result.focusAreas() != null && !result.focusAreas().isEmpty()) {
            Div focus = new Div();
            focus.addClassName("result-focus");
            focus.add(new H2("На что посмотреть"));
            Div grid = new Div();
            grid.addClassName("focus-area-grid");
            for (FocusAreaDto area : result.focusAreas()) {
                grid.add(focusCard(area));
            }
            focus.add(grid);
            add(focus);
        }

        Div details = new Div();
        details.addClassName("result-details");
        Div detailsHeader = new Div();
        detailsHeader.addClassName("result-details-header");
        detailsHeader.add(new H2("Разбор ошибок"));
        details.add(detailsHeader);

        List<PublicAnswerDetailDto> wrong = result.details() == null ? List.of()
                : result.details().stream().filter(d -> !d.isCorrect()).toList();
        if (wrong.isEmpty()) {
            Paragraph none = new Paragraph("Ошибок нет.");
            none.addClassName("result-no-mistakes");
            details.add(none);
        }
        UnorderedList list = new UnorderedList();
        list.addClassName("answer-review-list");
        for (PublicAnswerDetailDto d : wrong) {
            ListItem item = new ListItem();
            item.addClassName("answer-review-item");
            item.addClassName("answer-review-item-wrong");
            Paragraph cat = new Paragraph(d.categoryName());
            cat.addClassName("answer-review-category");
            Paragraph q = new Paragraph(d.questionText());
            q.addClassName("answer-review-question");
            Div comparison = new Div();
            comparison.addClassName("answer-review-comparison");
            comparison.add(new Paragraph("Твой ответ: " + (d.selectedOption() != null ? d.selectedOption() : "— не выбран")));
            item.add(cat, q, comparison);
            list.add(item);
        }
        details.add(list);
        add(details);

        Div actions = new Div();
        actions.addClassName("result-actions");
        actions.add(NativeUi.button("Пройти ещё раз", e -> getUI().ifPresent(ui -> ui.navigate(HomeView.class)),
                "btn", "btn-primary", "btn-large"));
        add(actions);
    }

    private Div focusCard(FocusAreaDto area) {
        Div card = new Div();
        card.addClassName("focus-area-card");
        card.add(new H3(area.categoryName()));
        card.add(new Paragraph(area.correctRate() + "% верных · " + area.wrongCount() + " "
                + RussianPlurals.mistakes(area.wrongCount())));
        if (area.prepLinks() != null && !area.prepLinks().isEmpty()) {
            UnorderedList links = new UnorderedList();
            for (PrepLinkDto link : area.prepLinks()) {
                Anchor a = new Anchor(link.url(), link.title());
                a.setTarget("_blank");
                a.getElement().setAttribute("rel", "noreferrer");
                links.add(new ListItem(a));
            }
            card.add(links);
        }
        return card;
    }
}
