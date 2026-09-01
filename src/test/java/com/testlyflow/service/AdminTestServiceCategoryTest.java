package com.testlyflow.service;

import com.testlyflow.dto.UploadTestResponse;
import com.testlyflow.entity.Category;
import com.testlyflow.entity.Test;
import com.testlyflow.parser.MarkdownTestParser;
import com.testlyflow.repository.TestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Ticket #1: an uploaded file's questions must end up attached to the resolved category. */
class AdminTestServiceCategoryTest {

    private TestRepository testRepository;
    private CategoryService categoryService;
    private AdminTestService adminTestService;

    private static final String SAMPLE_MD = """
            # Sample
            **1. Q1?**
            - А) a
            - Б) b

            ---

            ## Ключ ответов

            | № | Ответ |
            |---|---|
            | 1 | А |
            """;

    @BeforeEach
    void setUp() {
        testRepository = mock(TestRepository.class);
        categoryService = mock(CategoryService.class);
        adminTestService = new AdminTestService(testRepository, new MarkdownTestParser(), categoryService);

        when(testRepository.save(any(Test.class))).thenAnswer(inv -> {
            Test t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
    }

    @org.junit.jupiter.api.Test
    void everyParsedQuestionIsAttachedToTheResolvedCategory() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Флоу гашения");
        when(categoryService.resolveForUpload(eq(7L), isNull(), isNull(), isNull()))
                .thenReturn(new CategoryService.CategoryResolution(category, false));

        MockMultipartFile file = new MockMultipartFile("file", "sample.md", "text/markdown",
                SAMPLE_MD.getBytes(StandardCharsets.UTF_8));

        UploadTestResponse response = adminTestService.uploadTest(file, null, 7L, null, null, null);

        assertEquals(7L, response.categoryId());
        assertEquals("Флоу гашения", response.categoryName());
        assertEquals(false, response.categoryCreated());
        assertEquals(1, response.questionsAdded());

        ArgumentCaptor<Test> captor = ArgumentCaptor.forClass(Test.class);
        verify(testRepository).save(captor.capture());
        Test saved = captor.getValue();
        assertEquals(category, saved.getCategory());
        assertEquals(1, saved.getQuestions().size());
        assertEquals(category, saved.getQuestions().get(0).getCategory());
    }
}
