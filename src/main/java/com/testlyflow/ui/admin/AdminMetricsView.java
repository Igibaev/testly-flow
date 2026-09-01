package com.testlyflow.ui.admin;

import com.testlyflow.dto.AdminCategoryDto;
import com.testlyflow.dto.CategoryMetricsItemDto;
import com.testlyflow.dto.MetricsDto;
import com.testlyflow.dto.QuestionTimingItemDto;
import com.testlyflow.dto.TeamActivityDto;
import com.testlyflow.service.CategoryService;
import com.testlyflow.service.MetricsService;
import com.testlyflow.ui.component.BarRow;
import com.testlyflow.ui.component.MetricTile;
import com.testlyflow.ui.support.Formats;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Route(value = "admin/metrics", layout = AdminLayout.class)
@PageTitle("Метрики — админка")
public class AdminMetricsView extends Div {

    private final MetricsService metricsService;
    private final CategoryService categoryService;
    private final Select<AdminCategoryDto> categorySelect = new Select<>();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final Div body = new Div();
    private final Div errorBox = new Div();

    public AdminMetricsView(MetricsService metricsService, CategoryService categoryService) {
        this.metricsService = metricsService;
        this.categoryService = categoryService;
        add(new H1("Метрики"));

        List<AdminCategoryDto> categories = categoryService.listAdmin();
        categorySelect.setItems(categories);
        categorySelect.setPlaceholder("Все категории");
        categorySelect.setItemLabelGenerator(c -> c == null ? "Все категории" : c.name());
        categorySelect.setEmptySelectionAllowed(true);
        categorySelect.addValueChangeListener(e -> reload());
        from.addValueChangeListener(e -> reload());
        to.addValueChangeListener(e -> reload());

        Div filters = new Div();
        filters.addClassName("filters-row");
        NativeLabel fromLabel = new NativeLabel();
        fromLabel.addClassName("muted");
        fromLabel.getStyle().set("display", "flex").set("gap", "4px").set("align-items", "center");
        fromLabel.add(new Paragraph("с "), from);
        NativeLabel toLabel = new NativeLabel();
        toLabel.addClassName("muted");
        toLabel.getStyle().set("display", "flex").set("gap", "4px").set("align-items", "center");
        toLabel.add(new Paragraph("по "), to);
        filters.add(categorySelect, fromLabel, toLabel);
        add(filters);

        errorBox.addClassName("error-box");
        errorBox.setVisible(false);
        add(errorBox, body);
        reload();
    }

    private void reload() {
        body.removeAll();
        try {
            Long categoryId = categorySelect.getValue() == null ? null : categorySelect.getValue().id();
            OffsetDateTime fromTs = from.getValue() == null ? null : from.getValue().atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime toTs = to.getValue() == null ? null : to.getValue().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            render(metricsService.getMetrics(categoryId, fromTs, toTs));
            errorBox.setVisible(false);
        } catch (RuntimeException e) {
            errorBox.setText(e.getMessage());
            errorBox.setVisible(true);
        }
    }

    private void render(MetricsDto metrics) {
        Div tiles = new Div();
        tiles.addClassName("metrics-grid");
        tiles.add(new MetricTile(String.valueOf(metrics.startsCount()), "Стартов теста"));
        tiles.add(new MetricTile(String.valueOf(metrics.completedCount()), "Завершено"));
        tiles.add(new MetricTile(String.valueOf(metrics.abandonedCount()), "Не завершено"));
        tiles.add(new MetricTile(Formats.durationSeconds(metrics.averageDurationSeconds()), "Среднее время прохождения"));
        body.add(tiles);

        if (metrics.excludedSuspiciousAttempts() > 0) {
            Paragraph note = new Paragraph(
                    metrics.excludedSuspiciousAttempts()
                            + " попытк(и/а) с подозрительными таймингами исключены из расчёта времени (баллы учтены).");
            note.addClassName("muted");
            body.add(note);
        }

        Div dist = card("Распределение баллов",
                "Сколько завершённых попыток попало в каждый диапазон итогового балла.");
        long maxBucket = 1;
        for (Long v : metrics.scoreDistribution().values()) {
            maxBucket = Math.max(maxBucket, v);
        }
        for (Map.Entry<String, Long> entry : metrics.scoreDistribution().entrySet()) {
            dist.add(new BarRow(entry.getKey() + "%", entry.getValue() * 100.0 / maxBucket, String.valueOf(entry.getValue())));
        }
        body.add(dist);

        Div teams = card("Активность по командам", "Число завершённых попыток на команду.");
        if (metrics.teamActivity().isEmpty()) {
            Paragraph empty = new Paragraph("Нет данных.");
            empty.addClassName("muted");
            teams.add(empty);
        } else {
            long maxTeam = metrics.teamActivity().stream().mapToLong(TeamActivityDto::attempts).max().orElse(1);
            for (TeamActivityDto team : metrics.teamActivity()) {
                teams.add(new BarRow(team.team(), team.attempts() * 100.0 / maxTeam, String.valueOf(team.attempts())));
            }
        }
        body.add(teams);

        Div timing = card("Время на вопрос",
                "Считается только по завершённым попыткам без подозрительных таймингов; вопрос попадает в рейтинг только при "
                        + metrics.minSamplesForTiming() + "+ ответах.");
        if (metrics.questionTiming().slowestQuestions().isEmpty()) {
            Paragraph empty = new Paragraph(
                    "Недостаточно данных: нужно минимум " + metrics.minSamplesForTiming() + " ответов на вопрос.");
            empty.addClassName("muted");
            timing.add(empty);
        } else {
            Div tables = new Div();
            tables.addClassName("timing-tables");
            tables.add(timingTable("Самые долгие", metrics.questionTiming().slowestQuestions()));
            tables.add(timingTable("Самые быстрые", metrics.questionTiming().fastestQuestions()));
            timing.add(tables);
        }
        body.add(timing);

        Div blocks = card("По блокам", "Отсортировано по проблемности: дольше и с меньшей долей верных — выше.");
        if (metrics.categoryMetrics().isEmpty()) {
            Paragraph empty = new Paragraph("Недостаточно данных.");
            empty.addClassName("muted");
            blocks.add(empty);
        } else {
            List<CategoryMetricsItemDto> sorted = new ArrayList<>(metrics.categoryMetrics());
            sorted.sort(Comparator
                    .comparing(CategoryMetricsItemDto::avgTimePerQuestionSeconds, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(CategoryMetricsItemDto::correctRate, Comparator.nullsLast(Comparator.naturalOrder())));
            BigDecimal avg = metrics.questionTiming().averageTimePerQuestionSeconds();
            double scale = avg == null ? 1 : avg.doubleValue() * 2;
            if (scale <= 0) {
                scale = 1;
            }
            for (CategoryMetricsItemDto c : sorted) {
                Div card = new Div();
                card.addClassName("category-metric-card");
                card.getStyle().set("--cat-accent", c.color() == null ? "#6d5dfc" : c.color());
                card.add(new H3(c.categoryName()));
                double t = c.avgTimePerQuestionSeconds() == null ? 0 : c.avgTimePerQuestionSeconds().doubleValue();
                card.add(new BarRow("Время / вопрос", t / scale * 100,
                        c.avgTimePerQuestionSeconds() + " с (медиана " + c.medianTimePerQuestionSeconds() + " с)"));
                double rate = c.correctRate() == null ? 0 : c.correctRate().doubleValue();
                card.add(new BarRow("% верных", rate, c.correctRate() + "%", true));
                Paragraph meta = new Paragraph("Вопросов отвечено: " + c.questionsServed()
                        + " · Попыток охвачено: " + c.attemptsCovered()
                        + " · В среднем на блок: " + c.avgTimePerAttemptSeconds() + " с");
                meta.addClassName("muted");
                card.add(meta);
                blocks.add(card);
            }
        }
        body.add(blocks);
    }

    private Div timingTable(String title, List<QuestionTimingItemDto> items) {
        Div wrap = new Div();
        wrap.add(new H3(title));
        Grid<QuestionTimingItemDto> grid = new Grid<>(QuestionTimingItemDto.class, false);
        grid.addColumn(q -> "№" + q.number()).setHeader("Вопрос").setTooltipGenerator(QuestionTimingItemDto::text);
        grid.addColumn(QuestionTimingItemDto::categoryName).setHeader("Категория");
        grid.addColumn(q -> q.avgTimeSeconds() + " с / " + q.medianTimeSeconds() + " с").setHeader("Сред. / медиана");
        grid.addColumn(q -> q.correctRate() + "%").setHeader("% верных");
        grid.addColumn(QuestionTimingItemDto::samplesCount).setHeader("Наблюдений");
        grid.setItems(items);
        grid.setAllRowsVisible(true);
        wrap.add(grid);
        return wrap;
    }

    private Div card(String title, String subtitle) {
        Div card = new Div();
        card.addClassName("card");
        card.add(new H2(title));
        Paragraph p = new Paragraph(subtitle);
        p.addClassName("muted");
        card.add(p);
        return card;
    }
}
