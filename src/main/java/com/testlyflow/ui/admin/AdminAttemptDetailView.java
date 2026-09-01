package com.testlyflow.ui.admin;

import com.testlyflow.dto.AdminAttemptDetailDto;
import com.testlyflow.dto.AdminAttemptSummaryDto;
import com.testlyflow.dto.AnswerDetailDto;
import com.testlyflow.dto.CategoryTimeBreakdownDto;
import com.testlyflow.service.AdminAttemptService;
import com.testlyflow.ui.component.BarRow;
import com.testlyflow.ui.support.Formats;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "admin/attempts/:attemptId", layout = AdminLayout.class)
@PageTitle("Детализация попытки — админка")
public class AdminAttemptDetailView extends Div implements BeforeEnterObserver {

    private final AdminAttemptService adminAttemptService;

    public AdminAttemptDetailView(AdminAttemptService adminAttemptService) {
        this.adminAttemptService = adminAttemptService;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String raw = event.getRouteParameters().get("attemptId").orElse(null);
        try {
            render(adminAttemptService.getDetail(Long.valueOf(raw)));
        } catch (RuntimeException e) {
            Div box = new Div();
            box.addClassName("error-box");
            box.setText(e.getMessage());
            add(box);
        }
    }

    private void render(AdminAttemptDetailDto data) {
        AdminAttemptSummaryDto summary = data.summary();
        RouterLink back = new RouterLink("← К списку попыток", AdminAttemptsView.class);
        back.addClassName("muted");
        add(back);
        add(new H1(summary.lastName() + " " + summary.firstName() + " — " + summary.team()));

        Div card = new Div();
        card.addClassName("card");
        card.add(labeled("Статус:", "COMPLETED".equals(summary.status()) ? "Завершена" : "В процессе"));
        String result = summary.correctCount() != null
                ? summary.correctCount() + " / " + summary.totalQuestions() + " (" + Formats.percent(summary.scorePercent()) + ")"
                : "—";
        card.add(labeled("Результат:", result));
        card.add(labeled("Начало:", Formats.dateTime(summary.startedAt())));
        card.add(labeled("Завершение:", summary.finishedAt() == null ? "—" : Formats.dateTime(summary.finishedAt())));
        if (summary.timingSuspicious()) {
            Paragraph warn = new Paragraph(
                    "⚠ Сумма таймингов по вопросам заметно превышает длительность попытки — тайминги этой попытки исключены из метрик времени.");
            warn.addClassName("error-box");
            warn.getStyle().set("background", "#fff8e1").set("color", "#8a6d00").set("border-color", "#f0dca0");
            card.add(warn);
        }
        card.add(labeled("IP-адрес:", summary.ipAddress() == null ? "—" : summary.ipAddress()));
        card.add(labeled("User-Agent:", summary.userAgent() == null ? "—" : summary.userAgent()));
        add(card);

        if (data.slowestQuestion() != null) {
            AnswerDetailDto slow = data.slowestQuestion();
            Div slowCard = new Div();
            slowCard.addClassName("card");
            slowCard.add(new H2("Самый долгий вопрос в попытке"));
            slowCard.add(new Paragraph("№" + slow.number() + " · " + slow.categoryName() + " · "
                    + Formats.durationMs(slow.timeSpentMs())));
            Paragraph text = new Paragraph(slow.questionText());
            text.addClassName("muted");
            slowCard.add(text);
            add(slowCard);
        }

        if (data.categoryBreakdown() != null && !data.categoryBreakdown().isEmpty()) {
            Div breakCard = new Div();
            breakCard.addClassName("card");
            breakCard.add(new H2("Время по блокам"));
            for (CategoryTimeBreakdownDto c : data.categoryBreakdown()) {
                breakCard.add(new BarRow(c.categoryName(), 0,
                        Formats.durationMs(c.totalTimeSpentMs()) + " · " + c.questionCount() + " вопр."));
            }
            add(breakCard);
        }

        Div answers = new Div();
        answers.addClassName("card");
        answers.add(new H2("Детализация ответов"));
        for (AnswerDetailDto a : data.answers()) {
            Div row = new Div();
            row.addClassName("answer-row");
            Div left = new Div();
            Div number = new Div();
            number.addClassName("question-number");
            number.setText("Вопрос " + a.number() + " · " + a.categoryName() + " · " + Formats.durationMs(a.timeSpentMs()));
            left.add(number, new Div(a.questionText()));
            Paragraph muted = new Paragraph("Ответ: " + (a.selectedOption() == null ? "—" : a.selectedOption())
                    + " · Правильный: " + a.correctOption());
            muted.addClassName("muted");
            left.add(muted);
            Span badge = new Span(a.isCorrect() ? "Верно" : "Неверно");
            badge.addClassName("badge");
            badge.addClassName(a.isCorrect() ? "badge-correct" : "badge-incorrect");
            row.add(left, badge);
            answers.add(row);
        }
        add(answers);
    }

    private Paragraph labeled(String label, String value) {
        Paragraph paragraph = new Paragraph();
        Span strong = new Span(label);
        strong.getStyle().set("font-weight", "700");
        paragraph.add(strong, new Span(" " + value));
        return paragraph;
    }
}
