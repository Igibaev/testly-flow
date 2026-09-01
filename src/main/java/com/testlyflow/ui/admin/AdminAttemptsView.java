package com.testlyflow.ui.admin;

import com.testlyflow.dto.AdminAttemptSummaryDto;
import com.testlyflow.dto.PageResponse;
import com.testlyflow.service.AdminAttemptService;
import com.testlyflow.ui.support.Formats;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;

@Route(value = "admin/attempts", layout = AdminLayout.class)
@PageTitle("Попытки — админка")
public class AdminAttemptsView extends Div {

    private final AdminAttemptService adminAttemptService;
    private final Input teamFilter = new Input();
    private final Grid<AdminAttemptSummaryDto> grid = new Grid<>(AdminAttemptSummaryDto.class, false);
    private final Div errorBox = new Div();

    public AdminAttemptsView(AdminAttemptService adminAttemptService) {
        this.adminAttemptService = adminAttemptService;
        add(new H1("Попытки прохождения"));

        Div filters = new Div();
        filters.addClassName("filters-row");
        teamFilter.getElement().setAttribute("placeholder", "Команда");
        teamFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        filters.add(teamFilter);
        add(filters);

        errorBox.addClassName("error-box");
        errorBox.setVisible(false);
        add(errorBox);

        Div card = new Div();
        card.addClassName("card");
        grid.addColumn(a -> a.lastName() + " " + a.firstName()).setHeader("Участник").setAutoWidth(true);
        grid.addColumn(AdminAttemptSummaryDto::team).setHeader("Команда");
        grid.addColumn(a -> "COMPLETED".equals(a.status()) ? "Завершена" : "В процессе").setHeader("Статус");
        grid.addColumn(a -> a.scorePercent() == null ? "—" : Formats.percent(a.scorePercent())).setHeader("Балл");
        grid.addColumn(a -> Formats.dateTime(a.startedAt())).setHeader("Начало");
        grid.addComponentColumn(a -> {
            Span mark = new Span(a.timingSuspicious() ? "⚠" : "");
            mark.getElement().setAttribute("title", "Тайминги вызывают сомнение");
            return mark;
        }).setHeader("");
        grid.setPageSize(20);
        grid.addClassName("clickable-row");
        grid.addItemClickListener(e -> getUI().ifPresent(ui -> ui.navigate(AdminAttemptDetailView.class,
                new RouteParameters(new RouteParam("attemptId", String.valueOf(e.getItem().id()))))));
        grid.setItems(new CallbackDataProvider<>(query -> {
            int page = query.getOffset() / Math.max(query.getLimit(), 1);
            try {
                PageResponse<AdminAttemptSummaryDto> data = adminAttemptService.search(team(), page, query.getLimit());
                errorBox.setVisible(false);
                return data.content().stream();
            } catch (RuntimeException ex) {
                errorBox.setText(ex.getMessage());
                errorBox.setVisible(true);
                return java.util.stream.Stream.empty();
            }
        }, query -> {
            try {
                PageResponse<AdminAttemptSummaryDto> data = adminAttemptService.search(team(), 0, 20);
                return (int) data.totalElements();
            } catch (RuntimeException ex) {
                return 0;
            }
        }));
        card.add(grid);
        Paragraph emptyHint = new Paragraph();
        emptyHint.addClassName("muted");
        add(card);
    }

    private String team() {
        String value = teamFilter.getValue();
        return value == null || value.isBlank() ? null : value.trim();
    }
}
