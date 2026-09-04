package com.testlyflow.ui.admin;

import com.testlyflow.dto.AdminCategoryDto;
import com.testlyflow.dto.AdminTestSummaryDto;
import com.testlyflow.dto.UploadTestResponse;
import com.testlyflow.exception.TestParsingException;
import com.testlyflow.service.AdminTestService;
import com.testlyflow.service.CategoryService;
import com.testlyflow.ui.support.Formats;
import com.testlyflow.ui.support.NativeUi;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.util.List;

@Route(value = "admin/tests", layout = AdminLayout.class)
@PageTitle("Загрузка вопросов — админка")
public class AdminTestsView extends Div {

    private final AdminTestService adminTestService;
    private final CategoryService categoryService;

    private byte[] fileBytes;
    private final Input title = new Input();
    private final Select<AdminCategoryDto> existingCategory = new Select<>();
    private final Input newName = new Input();
    private final Input newDescription = new Input();
    private final Input newColor = new Input();
    private boolean createNew;
    private final Div errorBox = new Div();
    private final Div successBox = new Div();
    private final Div warningBox = new Div();
    private final Div listCard = new Div();
    private boolean uploading;

    public AdminTestsView(AdminTestService adminTestService, CategoryService categoryService) {
        this.adminTestService = adminTestService;
        this.categoryService = categoryService;

        errorBox.addClassName("error-box");
        errorBox.setId("upload-error");
        errorBox.setVisible(false);
        successBox.addClassName("error-box");
        successBox.getStyle().set("background", "#eefbf3").set("color", "#166534").set("border-color", "#bbf0d0");
        successBox.setVisible(false);
        warningBox.addClassName("error-box");
        warningBox.getStyle().set("background", "#fff8e1").set("color", "#8a6d00").set("border-color", "#f0dca0");
        warningBox.setVisible(false);

        newColor.getElement().setAttribute("type", "color");
        newColor.setValue("#6d5dfc");
        newName.setId("new-category-name");
        newName.getElement().setAttribute("placeholder", "Название новой категории");
        newDescription.getElement().setAttribute("placeholder", "Описание (необязательно)");

        Div card = new Div();
        card.addClassName("card");
        card.add(new H2("Загрузить вопросы из MD-файла"));
        card.add(errorBox, successBox, warningBox);
        card.add(buildForm());
        listCard.addClassName("card");
        add(card, listCard);
        reload();
    }

    private Div buildForm() {
        Div form = new Div();
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".md", ".markdown", "text/markdown", "text/plain");
        upload.setMaxFileSize(10 * 1024 * 1024);
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> {
            try {
                fileBytes = buffer.getInputStream().readAllBytes();
            } catch (IOException e) {
                showError("Не удалось прочитать файл");
            }
        });
        upload.addFileRejectedListener(event -> showError(event.getErrorMessage()));
        NativeLabel fileLabel = new NativeLabel();
        fileLabel.addClassName("form-field");
        fileLabel.add(new Span("MD-файл (вопросы + ключ ответов)"), upload);

        NativeLabel titleLabel = new NativeLabel();
        titleLabel.addClassName("form-field");
        titleLabel.add(new Span("Название загрузки (необязательно, иначе берётся из файла)"), title);

        NativeLabel categoryLabel = new NativeLabel();
        categoryLabel.addClassName("form-field");
        categoryLabel.add(new Span("Категория"));
        Div modes = new Div();
        modes.getStyle().set("display", "flex").set("gap", "16px").set("margin-bottom", "8px");
        var existingBtn = NativeUi.button("Выбрать существующую", e -> {
            createNew = false;
            refreshCategoryMode();
        }, "btn", "btn-secondary");
        var newBtn = NativeUi.button("Создать новую", e -> {
            createNew = true;
            refreshCategoryMode();
        }, "btn", "btn-secondary");
        modes.add(existingBtn, newBtn);
        categoryLabel.add(modes);

        existingCategory.setItemLabelGenerator(c -> c == null ? "— выберите —" : c.name());
        existingCategory.setPlaceholder("— выберите —");
        existingCategory.setWidthFull();

        Div newFields = new Div();
        newFields.setId("new-category-fields");
        newFields.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "8px");
        newFields.add(newName, newDescription, newColor);

        Div categoryHost = new Div();
        categoryHost.setId("category-host");
        categoryHost.add(existingCategory);
        categoryLabel.add(categoryHost);

        form.add(fileLabel, titleLabel, categoryLabel);
        form.add(NativeUi.button("Загрузить", e -> handleUpload(), "btn"));

        existingCategory.getElement().setProperty("id", "existing-category");
        form.getElement().setAttribute("data-create-new", "false");
        form.add(newFields);
        newFields.setVisible(false);
        this.newFields = newFields;
        this.existingHost = existingCategory;
        return form;
    }

    private Div newFields;
    private Select<AdminCategoryDto> existingHost;

    private void refreshCategoryMode() {
        if (newFields != null) {
            newFields.setVisible(createNew);
        }
        existingHost.setVisible(!createNew);
    }

    private void handleUpload() {
        if (fileBytes == null || fileBytes.length == 0) {
            showError("Выберите MD-файл с вопросами");
            return;
        }
        Long categoryId = null;
        String newCategoryName = null;
        String newCategoryDescription = null;
        String newCategoryColor = null;
        if (createNew) {
            if (newName.getValue() == null || newName.getValue().isBlank()) {
                showError("Укажите название новой категории");
                return;
            }
            newCategoryName = newName.getValue().trim();
            newCategoryDescription = blankToNull(newDescription.getValue());
            newCategoryColor = blankToNull(newColor.getValue());
        } else {
            AdminCategoryDto selected = existingCategory.getValue();
            if (selected == null) {
                showError("Выберите категорию, либо переключитесь на «Создать новую»");
                return;
            }
            categoryId = selected.id();
        }
        uploading = true;
        errorBox.setVisible(false);
        successBox.setVisible(false);
        warningBox.setVisible(false);
        try {
            UploadTestResponse response = adminTestService.uploadTest(
                    fileBytes,
                    blankToNull(title.getValue()),
                    categoryId,
                    newCategoryName,
                    newCategoryDescription,
                    newCategoryColor);
            successBox.setText("Добавлено " + response.questionsAdded() + " вопрос(ов) в категорию «"
                    + response.categoryName() + "»"
                    + (response.categoryCreated() ? " (создана новая категория)" : "") + ".");
            successBox.setVisible(true);
            if (response.warnings() != null && !response.warnings().isEmpty()) {
                warningBox.removeAll();
                warningBox.add(new Paragraph("Загружено с предупреждениями:"));
                UnorderedList list = new UnorderedList();
                for (String w : response.warnings()) {
                    list.add(new ListItem(w));
                }
                warningBox.add(list);
                warningBox.setVisible(true);
            }
            fileBytes = null;
            title.setValue("");
            newName.setValue("");
            newDescription.setValue("");
            reload();
        } catch (TestParsingException e) {
            showError(e.getMessage(), e.getDetails());
        } catch (RuntimeException e) {
            showError(e.getMessage());
        } finally {
            uploading = false;
        }
    }

    private void reload() {
        List<AdminCategoryDto> categories = categoryService.listAdmin();
        existingCategory.setItems(categories);
        List<AdminTestSummaryDto> tests = adminTestService.listTests();
        listCard.removeAll();
        listCard.add(new H2("Загруженные файлы"));
        if (tests.isEmpty()) {
            Paragraph empty = new Paragraph("Пока ничего не загружено.");
            empty.addClassName("muted");
            listCard.add(empty);
            return;
        }
        Grid<AdminTestSummaryDto> grid = new Grid<>(AdminTestSummaryDto.class, false);
        grid.addColumn(AdminTestSummaryDto::title).setHeader("Название");
        grid.addColumn(AdminTestSummaryDto::categoryName).setHeader("Категория");
        grid.addColumn(AdminTestSummaryDto::questionCount).setHeader("Вопросов");
        grid.addColumn(t -> Formats.dateTime(t.createdAt())).setHeader("Загружен");
        grid.addComponentColumn(t -> NativeUi.button("Удалить", e -> handleDelete(t), "btn", "btn-secondary"))
                .setHeader("");
        grid.setItems(tests);
        grid.setAllRowsVisible(true);
        listCard.add(grid);
    }

    private void handleDelete(AdminTestSummaryDto t) {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "return confirm($0);", "Удалить загрузку «" + t.title() + "» вместе со всеми её вопросами?")
                .then(Boolean.class, ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        try {
                            adminTestService.deleteTest(t.id());
                            reload();
                        } catch (RuntimeException e) {
                            showError(e.getMessage());
                        }
                    }
                }));
    }

    private void showError(String message) {
        showError(message, List.of());
    }

    private void showError(String message, List<String> details) {
        errorBox.removeAll();
        errorBox.add(new Paragraph(message));
        if (details != null && !details.isEmpty()) {
            UnorderedList list = new UnorderedList();
            for (String detail : details) {
                list.add(new ListItem(detail));
            }
            errorBox.add(list);
        }
        errorBox.setVisible(true);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
