package com.testlyflow.ui.view;

import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.testlyflow.dto.CategoryDto;
import com.testlyflow.dto.StartAttemptRequest;
import com.testlyflow.dto.StartAttemptResponse;
import com.testlyflow.service.AttemptService;
import com.testlyflow.service.CategoryService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeViewTest {

    private CategoryService categoryService;
    private AttemptService attemptService;

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
        categoryService = mock(CategoryService.class);
        attemptService = mock(AttemptService.class);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void rendersCategoryCards() {
        when(categoryService.listPublic()).thenReturn(List.of(
                new CategoryDto(1L, "Флоу гашения", "desc", "#c2410c", 12, List.of())));
        when(categoryService.estimateAttemptSize())
                .thenReturn(new CategoryService.AttemptSizeEstimate(10, 12, 1));

        UI.getCurrent().add(new HomeView(categoryService, attemptService));

        LocatorJ._assert(H3.class, 1, spec -> spec.withText("Флоу гашения"));
        LocatorJ._assert(NativeButton.class, 1, spec -> spec.withText("Пройти тест"));
    }

    @Test
    void hidesStartFormWhenNoCategories() {
        when(categoryService.listPublic()).thenReturn(List.of());
        when(categoryService.estimateAttemptSize())
                .thenReturn(new CategoryService.AttemptSizeEstimate(0, 0, 0));

        UI.getCurrent().add(new HomeView(categoryService, attemptService));

        LocatorJ._assertNone(NativeButton.class, spec -> spec.withText("Пройти тест"));
        verify(attemptService, never()).startAttempt(any(), any(), any());
    }

    @Test
    void emptyFieldsDoNotCallService() {
        when(categoryService.listPublic()).thenReturn(List.of(
                new CategoryDto(1L, "Блок", null, null, 4, List.of())));
        when(categoryService.estimateAttemptSize())
                .thenReturn(new CategoryService.AttemptSizeEstimate(4, 4, 1));

        UI.getCurrent().add(new HomeView(categoryService, attemptService));
        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Пройти тест")));

        verify(attemptService, never()).startAttempt(any(), any(), any());
        assertTrue(LocatorJ._get(com.vaadin.flow.component.html.Paragraph.class,
                spec -> spec.withText("Заполни имя, фамилию и команду")).isVisible());
    }

    @Test
    void successfulStartCallsService() {
        when(categoryService.listPublic()).thenReturn(List.of(
                new CategoryDto(1L, "Блок", null, null, 4, List.of())));
        when(categoryService.estimateAttemptSize())
                .thenReturn(new CategoryService.AttemptSizeEstimate(4, 4, 1));
        when(attemptService.startAttempt(any(), any(), any()))
                .thenReturn(new StartAttemptResponse(42L, OffsetDateTime.now(), List.of(), 4, 1));

        UI.getCurrent().add(new HomeView(categoryService, attemptService));
        LocatorJ._get(Input.class, spec -> spec.withId("firstName")).setValue("Иван");
        LocatorJ._get(Input.class, spec -> spec.withId("lastName")).setValue("Иванов");
        LocatorJ._get(Input.class, spec -> spec.withId("team")).setValue("Alpha");
        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Пройти тест")));

        verify(attemptService).startAttempt(eq(new StartAttemptRequest("Иван", "Иванов", "Alpha")), any(), any());
    }
}
