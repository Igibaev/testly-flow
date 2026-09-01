package com.testlyflow.ui.admin;

import com.testlyflow.dto.AdminCategoryDto;
import com.testlyflow.dto.CategoryUpsertRequest;
import com.testlyflow.dto.PrepLinkDto;
import com.testlyflow.dto.PrepLinkUpsertRequest;
import com.testlyflow.exception.ConflictException;
import com.testlyflow.service.CategoryService;
import com.testlyflow.ui.support.NativeUi;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "admin/categories", layout = AdminLayout.class)
@PageTitle("Категории — админка")
public class AdminCategoriesView extends Div {

    private final CategoryService categoryService;
    private Long editingId;
    private Long linkEditorId;
    private List<PrepLinkUpsertRequest> editingLinks = new ArrayList<>();

    private final Input name = new Input();
    private final Input description = new Input();
    private final Input color = new Input();
    private final Input questionsMin = new Input();
    private final Input questionsMax = new Input();
    private final Div errorBox = new Div();
    private final Div listCard = new Div();
    private final Div formCard = new Div();
    private final Div linkModalHost = new Div();
    private boolean saving;

    public AdminCategoriesView(CategoryService categoryService) {
        this.categoryService = categoryService;
        add(new H1("Категории вопросов"));
        errorBox.addClassName("error-box");
        errorBox.setVisible(false);
        color.getElement().setAttribute("type", "color");
        color.setValue("#6d5dfc");
        questionsMin.getElement().setAttribute("type", "number");
        questionsMin.getElement().setAttribute("min", "1");
        questionsMin.getElement().setAttribute("placeholder", "мин");
        questionsMax.getElement().setAttribute("type", "number");
        questionsMax.getElement().setAttribute("min", "1");
        questionsMax.getElement().setAttribute("placeholder", "макс");
        formCard.addClassName("card");
        listCard.addClassName("card");
        add(formCard, listCard, linkModalHost);
        rebuildForm();
        reload();
    }

    private void reload() {
        try {
            List<AdminCategoryDto> categories = categoryService.listAdmin();
            Map<Long, List<PrepLinkDto>> links = categoryService.prepLinksByCategory();
            rebuildTable(categories, links);
            errorBox.setVisible(false);
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void rebuildForm() {
        formCard.removeAll();
        formCard.add(new H2(editingId == null ? "Новая категория" : "Изменить категорию"));
        formCard.add(errorBox);
        formCard.add(field("Название", name));
        formCard.add(field("Описание", description));
        formCard.add(field("Цвет-акцент", color));
        NativeLabel range = new NativeLabel();
        range.addClassName("form-field");
        range.add(new Span("Вопросов в попытке: мин / макс (необязательно, иначе глобальный дефолт)"));
        Div row = new Div();
        row.getStyle().set("display", "flex").set("gap", "8px");
        row.add(questionsMin, questionsMax);
        range.add(row);
        formCard.add(range);
        formCard.add(NativeUi.button(saving ? "Сохраняем…" : (editingId == null ? "Создать" : "Сохранить"),
                e -> handleSubmit(), "btn"));
        if (editingId != null) {
            formCard.add(NativeUi.button("Отмена", e -> {
                editingId = null;
                resetForm();
                rebuildForm();
            }, "btn", "btn-secondary"));
        }
    }

    private NativeLabel field(String caption, Input input) {
        NativeLabel label = new NativeLabel();
        label.addClassName("form-field");
        label.add(new Span(caption), input);
        return label;
    }

    private void handleSubmit() {
        saving = true;
        try {
            CategoryUpsertRequest payload = new CategoryUpsertRequest(
                    nullToEmpty(name.getValue()).trim(),
                    blankToNull(description.getValue()),
                    blankToNull(color.getValue()),
                    null,
                    parseInt(questionsMin.getValue()),
                    parseInt(questionsMax.getValue()));
            if (editingId != null) {
                categoryService.update(editingId, payload);
            } else {
                categoryService.create(payload);
            }
            editingId = null;
            resetForm();
            reload();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        } finally {
            saving = false;
            rebuildForm();
        }
    }

    private void rebuildTable(List<AdminCategoryDto> categories, Map<Long, List<PrepLinkDto>> links) {
        listCard.removeAll();
        listCard.add(new H2("Все категории"));
        if (categories.isEmpty()) {
            Paragraph empty = new Paragraph("Категорий ещё нет.");
            empty.addClassName("muted");
            listCard.add(empty);
            return;
        }
        Grid<AdminCategoryDto> grid = new Grid<>(AdminCategoryDto.class, false);
        grid.addComponentColumn(c -> {
            Div cell = new Div();
            Span dot = new Span();
            dot.addClassName("color-dot");
            dot.getStyle().set("background", c.color() == null ? "#94a3b8" : c.color());
            cell.add(dot, new Span(" " + c.name()));
            return cell;
        }).setHeader("Название");
        grid.addColumn(AdminCategoryDto::questionCount).setHeader("Вопросов");
        grid.addColumn(AdminCategoryDto::testCount).setHeader("Загрузок");
        grid.addColumn(c -> (c.questionsMin() == null ? "—" : c.questionsMin()) + "–"
                        + (c.questionsMax() == null ? "—" : c.questionsMax()))
                .setHeader("Диапазон выборки");
        grid.addComponentColumn(c -> {
            Div actions = new Div();
            actions.getStyle().set("display", "flex").set("gap", "8px");
            actions.add(NativeUi.button("Изменить", e -> startEdit(c), "btn", "btn-secondary"));
            int linkCount = links.getOrDefault(c.id(), List.of()).size();
            actions.add(NativeUi.button("Ссылки (" + linkCount + ")",
                    e -> openLinkEditor(c, links.getOrDefault(c.id(), List.of())),
                    "btn", "btn-secondary"));
            var delete = NativeUi.button("Удалить", e -> handleDelete(c), "btn", "btn-secondary");
            delete.setEnabled(c.questionCount() == 0);
            if (c.questionCount() > 0) {
                delete.getElement().setAttribute("title", "Нельзя удалить: в категории есть вопросы");
            }
            actions.add(delete);
            return actions;
        }).setHeader("");
        grid.setItems(categories);
        grid.setAllRowsVisible(true);
        listCard.add(grid);
    }

    private void startEdit(AdminCategoryDto c) {
        editingId = c.id();
        name.setValue(c.name());
        description.setValue(c.description() == null ? "" : c.description());
        color.setValue(c.color() == null ? "#6d5dfc" : c.color());
        questionsMin.setValue(c.questionsMin() == null ? "" : String.valueOf(c.questionsMin()));
        questionsMax.setValue(c.questionsMax() == null ? "" : String.valueOf(c.questionsMax()));
        rebuildForm();
    }

    private void handleDelete(AdminCategoryDto c) {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "return confirm($0);", "Удалить категорию «" + c.name() + "»?")
                .then(Boolean.class, ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        try {
                            categoryService.delete(c.id());
                            reload();
                        } catch (RuntimeException e) {
                            showError(e.getMessage());
                        }
                    }
                }));
    }

    private void openLinkEditor(AdminCategoryDto c, List<PrepLinkDto> existing) {
        linkEditorId = c.id();
        editingLinks = new ArrayList<>();
        if (existing.isEmpty()) {
            editingLinks.add(new PrepLinkUpsertRequest("", ""));
        } else {
            for (PrepLinkDto l : existing) {
                editingLinks.add(new PrepLinkUpsertRequest(l.title(), l.url()));
            }
        }
        renderLinkModal();
    }

    private void renderLinkModal() {
        linkModalHost.removeAll();
        if (linkEditorId == null) {
            return;
        }
        Div backdrop = new Div();
        backdrop.addClassName("modal-backdrop");
        backdrop.getElement().setAttribute("role", "dialog");
        backdrop.getElement().setAttribute("aria-modal", "true");
        Div modal = new Div();
        modal.addClassName("modal");
        modal.add(new H3("Подготовительные ссылки"));
        for (int i = 0; i < editingLinks.size(); i++) {
            int index = i;
            PrepLinkUpsertRequest link = editingLinks.get(i);
            Div row = new Div();
            row.getStyle().set("display", "flex").set("gap", "8px").set("margin-bottom", "6px");
            Input title = new Input();
            title.getElement().setAttribute("placeholder", "Заголовок");
            title.setValue(link.title() == null ? "" : link.title());
            title.addValueChangeListener(e -> editingLinks.set(index,
                    new PrepLinkUpsertRequest(e.getValue(), editingLinks.get(index).url())));
            Input url = new Input();
            url.getElement().setAttribute("placeholder", "URL");
            url.setValue(link.url() == null ? "" : link.url());
            url.addValueChangeListener(e -> editingLinks.set(index,
                    new PrepLinkUpsertRequest(editingLinks.get(index).title(), e.getValue())));
            int removeAt = i;
            row.add(title, url, NativeUi.button("✕", ev -> {
                editingLinks.remove(removeAt);
                if (editingLinks.isEmpty()) {
                    editingLinks.add(new PrepLinkUpsertRequest("", ""));
                }
                renderLinkModal();
            }, "btn", "btn-secondary"));
            modal.add(row);
        }
        modal.add(NativeUi.button("+ Добавить ссылку", e -> {
            editingLinks.add(new PrepLinkUpsertRequest("", ""));
            renderLinkModal();
        }, "btn", "btn-secondary"));
        Div actions = new Div();
        actions.addClassName("modal-actions");
        actions.add(NativeUi.button("Отмена", e -> {
            linkEditorId = null;
            renderLinkModal();
        }, "btn", "btn-secondary"));
        actions.add(NativeUi.button("Сохранить", e -> saveLinks(), "btn"));
        modal.add(actions);
        backdrop.add(modal);
        linkModalHost.add(backdrop);
    }

    private void saveLinks() {
        List<PrepLinkUpsertRequest> filtered = editingLinks.stream()
                .filter(l -> l.title() != null && !l.title().isBlank() && l.url() != null && !l.url().isBlank())
                .toList();
        try {
            categoryService.updatePrepLinks(linkEditorId, filtered);
            linkEditorId = null;
            renderLinkModal();
            reload();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void resetForm() {
        name.setValue("");
        description.setValue("");
        color.setValue("#6d5dfc");
        questionsMin.setValue("");
        questionsMax.setValue("");
    }

    private void showError(String message) {
        errorBox.setText(message);
        errorBox.setVisible(true);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
