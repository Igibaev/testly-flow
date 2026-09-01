package com.testlyflow.ui.admin;

import com.testlyflow.dto.EmployeeSummaryDto;
import com.testlyflow.service.AdminEmployeeService;
import com.testlyflow.ui.support.Formats;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

import java.util.List;

@Route(value = "admin/employees", layout = AdminLayout.class)
@PageTitle("Сотрудники — админка")
public class AdminEmployeesView extends Div {

    private final AdminEmployeeService employeeService;
    private final Input search = new Input();
    private final Div tableHost = new Div();
    private List<EmployeeSummaryDto> employees = List.of();

    public AdminEmployeesView(AdminEmployeeService employeeService) {
        this.employeeService = employeeService;
        add(new H1("Сотрудники"));
        Paragraph hint = new Paragraph(
                "Отсортировано по среднему времени на вопрос — быстрее всех наверху. Учитываются только завершённые попытки без подозрительных таймингов.");
        hint.addClassName("muted");
        add(hint);

        Div filters = new Div();
        filters.addClassName("filters-row");
        search.getElement().setAttribute("placeholder", "Поиск по имени или команде");
        search.addValueChangeListener(e -> renderTable());
        filters.add(search);
        add(filters);

        tableHost.addClassName("card");
        add(tableHost);
        try {
            employees = employeeService.roster();
            renderTable();
        } catch (RuntimeException e) {
            Div box = new Div();
            box.addClassName("error-box");
            box.setText(e.getMessage());
            add(box);
        }
    }

    private void renderTable() {
        tableHost.removeAll();
        String q = search.getValue() == null ? "" : search.getValue().toLowerCase();
        List<EmployeeSummaryDto> filtered = employees.stream()
                .filter(e -> (e.firstName() + " " + e.lastName() + " " + e.team()).toLowerCase().contains(q))
                .toList();
        Grid<EmployeeSummaryDto> grid = new Grid<>(EmployeeSummaryDto.class, false);
        grid.addComponentColumn(e -> new RouterLink(
                e.lastName() + " " + e.firstName(),
                AdminEmployeeCardView.class,
                new RouteParameters(
                        new RouteParam("firstName", e.firstName()),
                        new RouteParam("lastName", e.lastName()),
                        new RouteParam("team", e.team()))))
                .setHeader("Сотрудник");
        grid.addColumn(EmployeeSummaryDto::team).setHeader("Команда");
        grid.addColumn(EmployeeSummaryDto::attemptsCount).setHeader("Попыток");
        grid.addColumn(EmployeeSummaryDto::completedCount).setHeader("Завершено");
        grid.addColumn(e -> e.avgScorePercent() == null ? "—" : Formats.percent(e.avgScorePercent())).setHeader("Ср. балл");
        grid.addColumn(e -> e.avgTimePerQuestionSeconds() == null ? "—" : e.avgTimePerQuestionSeconds() + " с")
                .setHeader("Ср. время / вопрос");
        grid.addColumn(e -> Formats.dateTime(e.lastAttemptAt())).setHeader("Последняя попытка");
        grid.setItems(filtered);
        grid.setAllRowsVisible(true);
        tableHost.add(grid);
        if (filtered.isEmpty()) {
            Paragraph empty = new Paragraph("Никого не найдено.");
            empty.addClassName("muted");
            tableHost.add(empty);
        }
    }
}
