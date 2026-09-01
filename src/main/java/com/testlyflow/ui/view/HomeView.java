package com.testlyflow.ui.view;

import com.testlyflow.dto.CategoryDto;
import com.testlyflow.dto.PrepLinkDto;
import com.testlyflow.dto.StartAttemptRequest;
import com.testlyflow.dto.StartAttemptResponse;
import com.testlyflow.service.CategoryService;
import com.testlyflow.service.AttemptService;
import com.testlyflow.ui.MainLayout;
import com.testlyflow.ui.support.ClientInfoResolver;
import com.testlyflow.ui.support.NativeUi;
import com.testlyflow.ui.support.RussianPlurals;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Платформа тестирования знаний")
public class HomeView extends Div {

    private final CategoryService categoryService;
    private final AttemptService attemptService;

    private final Input firstName = new Input();
    private final Input lastName = new Input();
    private final Input team = new Input();
    private final Paragraph startError = new Paragraph();
    private boolean starting;

    public HomeView(CategoryService categoryService, AttemptService attemptService) {
        this.categoryService = categoryService;
        this.attemptService = attemptService;
        addClassName("home-page");
        startError.addClassName("field-error");
        startError.setVisible(false);
        render();
    }

    private void render() {
        removeAll();
        add(intro());

        Div categoriesSection = new Div();
        categoriesSection.addClassName("home-categories");
        categoriesSection.getElement().setAttribute("aria-live", "polite");
        categoriesSection.add(sectionHeader("Категории вопросов", "Добавленные блоки, из которых собирается тест"));

        try {
            List<CategoryDto> categories = categoryService.listPublic();
            if (categories.isEmpty()) {
                Div empty = new Div();
                empty.addClassName("state-empty");
                empty.add(new Paragraph("Пока нет ни одного блока с вопросами. Загрузите вопросы в админ-панели."));
                categoriesSection.add(empty);
            } else {
                categoriesSection.add(categoryGrid(categories));
            }
            add(categoriesSection);

            CategoryService.AttemptSizeEstimate estimate = categoryService.estimateAttemptSize();
            if (estimate.blockCount() > 0) {
                add(startForm(estimate));
            }
        } catch (RuntimeException ex) {
            Div error = new Div();
            error.addClassName("state-error");
            error.add(new Paragraph("Не удалось загрузить блоки вопросов: " + ex.getMessage()));
            error.add(NativeUi.button("Повторить", e -> render(), "btn", "btn-secondary"));
            categoriesSection.add(error);
            add(categoriesSection);
        }
    }

    private Div intro() {
        Div section = new Div();
        section.addClassName("home-intro");
        section.add(new H1("Проверь себя"));
        Paragraph lede = new Paragraph(
                "Тест собирается из нескольких блоков вопросов. Из каждого блока попадёт по 10–15 "
                        + "случайных вопросов — состав каждый раз немного разный. Можно свободно "
                        + "возвращаться к вопросам и менять ответы, пока не нажмёшь «Завершить».");
        lede.addClassName("home-lede");
        section.add(lede);
        return section;
    }

    private Div sectionHeader(String title, String sub) {
        Div header = new Div();
        header.addClassName("home-section-header");
        header.add(new H2(title));
        Paragraph p = new Paragraph(sub);
        p.addClassName("home-section-sub");
        header.add(p);
        return header;
    }

    private Div categoryGrid(List<CategoryDto> categories) {
        Div grid = new Div();
        grid.addClassName("category-grid");
        for (CategoryDto category : categories) {
            grid.add(categoryCard(category));
        }
        return grid;
    }

    private Div categoryCard(CategoryDto category) {
        Div card = new Div();
        card.addClassName("category-card");
        String accent = category.color() != null && !category.color().isBlank()
                ? category.color() : "var(--color-accent)";
        card.getStyle().set("--cat-accent", accent);
        card.add(new H3(category.name()));
        if (category.description() != null && !category.description().isBlank()) {
            Paragraph desc = new Paragraph(category.description());
            desc.addClassName("category-card-desc");
            card.add(desc);
        }
        Paragraph count = new Paragraph(
                category.questionCount() + " " + RussianPlurals.questions((int) category.questionCount()) + " в пуле");
        count.addClassName("category-card-count");
        card.add(count);
        if (category.prepLinks() != null && !category.prepLinks().isEmpty()) {
            UnorderedList links = new UnorderedList();
            links.addClassName("category-card-links");
            for (PrepLinkDto link : category.prepLinks()) {
                Anchor a = new Anchor(link.url(), link.title());
                a.setTarget("_blank");
                a.getElement().setAttribute("rel", "noreferrer");
                links.add(new ListItem(a));
            }
            card.add(links);
        }
        return card;
    }

    private Div startForm(CategoryService.AttemptSizeEstimate estimate) {
        Div section = new Div();
        section.addClassName("home-start");

        Div form = new Div();
        form.addClassName("start-form");

        Div header = new Div();
        header.addClassName("home-section-header");
        header.add(new H2("Начать тест"));
        Paragraph hint = new Paragraph(estimateText(estimate));
        hint.addClassName("start-form-hint");
        header.add(hint);
        form.add(header);

        firstName.setId("firstName");
        firstName.getElement().setAttribute("autocomplete", "given-name");
        lastName.setId("lastName");
        lastName.getElement().setAttribute("autocomplete", "family-name");
        team.setId("team");
        team.getElement().setAttribute("autocomplete", "organization");

        Div row = new Div();
        row.addClassName("field-row");
        row.add(labeledField("Имя", firstName), labeledField("Фамилия", lastName));
        form.add(row, labeledField("Команда", team), startError);

        var submit = NativeUi.button("Пройти тест", e -> handleStart(), "btn", "btn-primary", "btn-large", "start-form-submit");
        form.add(submit);
        section.add(form);
        return section;
    }

    private NativeLabel labeledField(String caption, Input input) {
        NativeLabel label = new NativeLabel();
        label.addClassName("field");
        Span span = new Span(caption);
        label.add(span, input);
        return label;
    }

    private String estimateText(CategoryService.AttemptSizeEstimate estimate) {
        int min = estimate.minQuestions();
        int max = estimate.maxQuestions();
        int blocks = estimate.blockCount();
        String blockWord = RussianPlurals.blocksGenitive(blocks);
        if (min == max) {
            return "Тест соберёт " + min + " " + RussianPlurals.questions(min) + " из " + blocks + " " + blockWord + ".";
        }
        return "Тест соберёт " + min + "–" + max + " " + RussianPlurals.questions(max)
                + " из " + blocks + " " + blockWord + ".";
    }

    private void handleStart() {
        if (starting) {
            return;
        }
        String fn = nullToEmpty(firstName.getValue()).trim();
        String ln = nullToEmpty(lastName.getValue()).trim();
        String tm = nullToEmpty(team.getValue()).trim();
        if (fn.isEmpty() || ln.isEmpty() || tm.isEmpty()) {
            startError.setText("Заполни имя, фамилию и команду");
            startError.setVisible(true);
            return;
        }
        starting = true;
        startError.setVisible(false);
        try {
            StartAttemptResponse data = attemptService.startAttempt(
                    new StartAttemptRequest(fn, ln, tm),
                    ClientInfoResolver.ip(),
                    ClientInfoResolver.userAgent());
            getUI().ifPresent(ui -> ui.navigate(AttemptView.class,
                    new RouteParameters("attemptId", String.valueOf(data.attemptId()))));
        } catch (RuntimeException ex) {
            startError.setText(ex.getMessage());
            startError.setVisible(true);
            starting = false;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
