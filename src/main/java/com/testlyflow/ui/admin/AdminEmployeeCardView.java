package com.testlyflow.ui.admin;

import com.testlyflow.dto.EmployeeAttemptDto;
import com.testlyflow.dto.EmployeeCardDto;
import com.testlyflow.dto.EmployeeQuestionTimingDto;
import com.testlyflow.service.AdminEmployeeService;
import com.testlyflow.ui.component.MetricTile;
import com.testlyflow.ui.support.Formats;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Route(value = "admin/employees/:firstName/:lastName/:team", layout = AdminLayout.class)
@PageTitle("Карточка сотрудника — админка")
public class AdminEmployeeCardView extends Div implements BeforeEnterObserver {

    private final AdminEmployeeService employeeService;

    public AdminEmployeeCardView(AdminEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String firstName = decode(event.getRouteParameters().get("firstName").orElse(""));
        String lastName = decode(event.getRouteParameters().get("lastName").orElse(""));
        String team = decode(event.getRouteParameters().get("team").orElse(""));
        try {
            render(employeeService.getCard(firstName, lastName, team));
        } catch (RuntimeException e) {
            Div box = new Div();
            box.addClassName("error-box");
            box.setText(e.getMessage());
            add(box);
        }
    }

    private void render(EmployeeCardDto card) {
        RouterLink back = new RouterLink("← К списку сотрудников", AdminEmployeesView.class);
        back.addClassName("muted");
        add(back);
        add(new H1(card.lastName() + " " + card.firstName() + " — " + card.team()));

        Div tiles = new Div();
        tiles.addClassName("metrics-grid");
        long completed = card.attempts().stream().filter(a -> "COMPLETED".equals(a.status())).count();
        tiles.add(new MetricTile(String.valueOf(card.attempts().size()), "Попыток"));
        tiles.add(new MetricTile(String.valueOf(completed), "Завершено"));
        tiles.add(new MetricTile(
                card.avgTimePerQuestionSeconds() == null ? "—" : card.avgTimePerQuestionSeconds() + " с",
                "Ср. время на вопрос"));
        add(tiles);

        Div timings = new Div();
        timings.addClassName("card");
        timings.add(new H2("Время по вопросам"));
        Paragraph hint = new Paragraph(
                "Сравнение со средним временем всех сотрудников на этот же вопрос (без порога минимума наблюдений — это справочная база для сравнения, а не рейтинг вопросов).");
        hint.addClassName("muted");
        timings.add(hint);
        if (card.questionTimings().isEmpty()) {
            Paragraph empty = new Paragraph("У этого сотрудника пока нет завершённых ответов с таймингом.");
            empty.addClassName("muted");
            timings.add(empty);
        } else {
            Grid<EmployeeQuestionTimingDto> grid = new Grid<>(EmployeeQuestionTimingDto.class, false);
            grid.addColumn(q -> "№" + q.number()).setHeader("Вопрос").setTooltipGenerator(EmployeeQuestionTimingDto::text);
            grid.addColumn(EmployeeQuestionTimingDto::categoryName).setHeader("Категория");
            grid.addColumn(q -> q.employeeAvgSeconds() + " с" + (q.employeeSamples() > 1 ? " (×" + q.employeeSamples() + ")" : ""))
                    .setHeader("Время сотрудника");
            grid.addColumn(q -> q.globalAvgSeconds() == null ? "—" : q.globalAvgSeconds() + " с (n=" + q.globalSamples() + ")")
                    .setHeader("В среднем у всех");
            grid.addColumn(AdminEmployeeCardView::renderDelta).setHeader("Разница");
            grid.addColumn(q -> q.employeeCorrectRate() + "%").setHeader("% верных у сотрудника");
            grid.setItems(card.questionTimings());
            grid.setAllRowsVisible(true);
            timings.add(grid);
        }
        add(timings);

        Div attempts = new Div();
        attempts.addClassName("card");
        attempts.add(new H2("Попытки"));
        Grid<EmployeeAttemptDto> grid = new Grid<>(EmployeeAttemptDto.class, false);
        grid.addColumn(a -> Formats.dateTime(a.startedAt())).setHeader("Начало");
        grid.addColumn(a -> "COMPLETED".equals(a.status()) ? "Завершена" : "В процессе").setHeader("Статус");
        grid.addColumn(a -> a.scorePercent() == null ? "—" : Formats.percent(a.scorePercent())).setHeader("Балл");
        grid.addComponentColumn(a -> {
            Div cell = new Div();
            cell.add(new RouterLink("Детали", AdminAttemptDetailView.class,
                    new RouteParameters(new RouteParam("attemptId", String.valueOf(a.id())))));
            if (a.timingSuspicious()) {
                Span mark = new Span(" ⚠");
                mark.getElement().setAttribute("title", "Тайминги вызывают сомнение");
                cell.add(mark);
            }
            return cell;
        }).setHeader("");
        grid.setItems(card.attempts());
        grid.setAllRowsVisible(true);
        attempts.add(grid);
        add(attempts);
    }

    static String renderDelta(EmployeeQuestionTimingDto q) {
        if (q.globalAvgSeconds() == null || q.globalAvgSeconds().compareTo(BigDecimal.ZERO) == 0) {
            return "—";
        }
        int diffPercent = q.employeeAvgSeconds()
                .subtract(q.globalAvgSeconds())
                .multiply(BigDecimal.valueOf(100))
                .divide(q.globalAvgSeconds(), 0, java.math.RoundingMode.HALF_UP)
                .intValue();
        if (Math.abs(diffPercent) < 5) {
            return "как у всех";
        }
        return diffPercent < 0 ? "быстрее на " + Math.abs(diffPercent) + "%" : "медленнее на " + diffPercent + "%";
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
