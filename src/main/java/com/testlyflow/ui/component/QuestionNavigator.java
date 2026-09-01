package com.testlyflow.ui.component;

import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.ui.support.NativeUi;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.UnorderedList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

public class QuestionNavigator extends Div {

    private static final String[] COLOR_PALETTE = {
            "#6d5dfc", "#0f9d8c", "#c2410c", "#0369a1", "#a21caf", "#65760a"
    };

    public QuestionNavigator(List<AttemptQuestionDto> questions,
                             Map<Long, String> answers,
                             Set<Integer> visited,
                             int currentIndex,
                             boolean open,
                             IntConsumer onGoTo) {
        addClassName("question-navigator");
        if (open) {
            addClassName("question-navigator-open");
        }
        getElement().setAttribute("aria-label", "Список вопросов");

        for (CategoryGroup group : groupByCategory(questions)) {
            Div block = new Div();
            block.addClassName("navigator-group");
            H4 heading = new H4(group.categoryName);
            heading.getStyle().set("--cat-accent", group.color);
            block.add(heading);

            Div grid = new Div();
            grid.addClassName("navigator-grid");
            for (IndexedQuestion item : group.items) {
                boolean answered = answers.get(item.question.questionId()) != null
                        && !answers.get(item.question.questionId()).isBlank();
                boolean isVisited = visited.contains(item.index);
                boolean isCurrent = item.index == currentIndex;
                String status = isCurrent ? "current" : answered ? "answered" : isVisited ? "skipped" : "unvisited";

                NativeButton cell = NativeUi.button(String.valueOf(item.question.displayNumber()),
                        e -> onGoTo.accept(item.index), "nav-cell", "nav-cell-" + status);
                if (isCurrent) {
                    cell.getElement().setAttribute("aria-current", "true");
                }
                cell.getElement().setAttribute("title", statusLabel(status));
                grid.add(cell);
            }
            block.add(grid);
            add(block);
        }

        UnorderedList legend = new UnorderedList();
        legend.addClassName("navigator-legend");
        legend.add(legendItem("nav-cell-answered", "Отвечен"));
        legend.add(legendItem("nav-cell-skipped", "Пропущен, посещён"));
        legend.add(legendItem("nav-cell-unvisited", "Не посещён"));
        legend.add(legendItem("nav-cell-current", "Текущий"));
        add(legend);
    }

    public static String colorFor(Long categoryId) {
        if (categoryId == null) {
            return "var(--color-accent)";
        }
        int idx = (int) (Math.abs(categoryId) % COLOR_PALETTE.length);
        return COLOR_PALETTE[idx];
    }

    private static ListItem legendItem(String className, String text) {
        ListItem item = new ListItem(text);
        item.addClassName(className);
        return item;
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "answered" -> "Отвечен";
            case "skipped" -> "Пропущен, посещён";
            case "unvisited" -> "Не посещён";
            default -> "Текущий вопрос";
        };
    }

    public static List<CategoryGroup> groupByCategory(List<AttemptQuestionDto> questions) {
        Map<Long, CategoryGroup> byId = new LinkedHashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            AttemptQuestionDto q = questions.get(i);
            CategoryGroup group = byId.computeIfAbsent(q.categoryId(),
                    id -> new CategoryGroup(id, q.categoryName(), colorFor(id), new ArrayList<>()));
            group.items.add(new IndexedQuestion(i, q));
        }
        return new ArrayList<>(byId.values());
    }

    public static final class CategoryGroup {
        public final Long categoryId;
        public final String categoryName;
        public final String color;
        public final List<IndexedQuestion> items;

        CategoryGroup(Long categoryId, String categoryName, String color, List<IndexedQuestion> items) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.color = color;
            this.items = items;
        }
    }

    public static final class IndexedQuestion {
        public final int index;
        public final AttemptQuestionDto question;

        IndexedQuestion(int index, AttemptQuestionDto question) {
            this.index = index;
            this.question = question;
        }
    }
}
