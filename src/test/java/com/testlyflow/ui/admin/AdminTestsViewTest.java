package com.testlyflow.ui.admin;

import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.testlyflow.dto.AdminCategoryDto;
import com.testlyflow.exception.TestParsingException;
import com.testlyflow.service.AdminTestService;
import com.testlyflow.service.CategoryService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminTestsViewTest {

    private AdminTestService adminTestService;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
        adminTestService = mock(AdminTestService.class);
        categoryService = mock(CategoryService.class);
        when(categoryService.listAdmin()).thenReturn(List.of(
                new AdminCategoryDto(1L, "Флоу", "flou", "", "#000", 0, 0, 0, null, null)));
        when(adminTestService.listTests()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void showsParserDetailsWhenQuestionsDoNotMatchTheAnswerKey() throws Exception {
        when(adminTestService.uploadTest(
                any(),
                nullable(String.class),
                nullable(Long.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class)))
                .thenThrow(new TestParsingException(
                        "Не удалось связать вопросы с ключом ответов",
                        List.of("Для вопроса №2 не найден ответ в ключе ответов")));

        AdminTestsView view = new AdminTestsView(adminTestService, categoryService);
        UI.getCurrent().add(view);
        setFileBytes(view, "# t\n".getBytes(StandardCharsets.UTF_8));

        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Создать новую")));
        LocatorJ._get(Input.class, spec -> spec.withId("new-category-name")).setValue("Новый блок");
        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Загрузить")));

        Div error = LocatorJ._get(Div.class, spec -> spec.withId("upload-error"));
        assertTrue(error.isVisible());
        LocatorJ._assert(ListItem.class, 1,
                spec -> spec.withText("Для вопроса №2 не найден ответ в ключе ответов"));
    }

    private static void setFileBytes(AdminTestsView view, byte[] bytes) throws Exception {
        Field field = AdminTestsView.class.getDeclaredField("fileBytes");
        field.setAccessible(true);
        field.set(view, bytes);
    }
}
