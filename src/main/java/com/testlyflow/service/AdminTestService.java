package com.testlyflow.service;

import com.testlyflow.dto.AdminTestSummaryDto;
import com.testlyflow.dto.UploadTestResponse;
import com.testlyflow.entity.Category;
import com.testlyflow.entity.Question;
import com.testlyflow.entity.QuestionOption;
import com.testlyflow.entity.Test;
import com.testlyflow.parser.MarkdownTestParser;
import com.testlyflow.parser.ParsedQuestion;
import com.testlyflow.parser.ParsedTestResult;
import com.testlyflow.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminTestService {

    private final TestRepository testRepository;
    private final MarkdownTestParser parser;
    private final CategoryService categoryService;

    public AdminTestService(TestRepository testRepository,
                             MarkdownTestParser parser,
                             CategoryService categoryService) {
        this.testRepository = testRepository;
        this.parser = parser;
        this.categoryService = categoryService;
    }

    @Transactional
    public UploadTestResponse uploadTest(byte[] fileBytes, String titleParam,
                                          Long categoryId, String newCategoryName,
                                          String newCategoryDescription, String newCategoryColor) {
        ParsedTestResult parsed = parser.parse(fileBytes);

        String title = (titleParam != null && !titleParam.isBlank()) ? titleParam.trim() : parsed.getTitleFromFile();
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Не указано название теста: передайте параметр title или добавьте заголовок \"# ...\" в начало файла");
        }

        CategoryService.CategoryResolution resolution =
                categoryService.resolveForUpload(categoryId, newCategoryName, newCategoryDescription, newCategoryColor);
        Category category = resolution.category();

        Test test = new Test();
        test.setTitle(title);
        test.setCategory(category);

        for (ParsedQuestion pq : parsed.getQuestions()) {
            Question question = new Question();
            question.setTest(test);
            question.setCategory(category);
            question.setNumber(pq.getNumber());
            question.setText(pq.getText());
            question.setCorrectOption(pq.getCorrectOption());
            pq.getOptions().forEach((letter, text) -> {
                QuestionOption option = new QuestionOption();
                option.setQuestion(question);
                option.setOptionLetter(letter);
                option.setText(text);
                question.getOptions().add(option);
            });
            test.getQuestions().add(question);
        }

        test = testRepository.save(test);

        AdminTestSummaryDto summary = new AdminTestSummaryDto(
                test.getId(), test.getTitle(), test.getDescription(),
                category.getId(), category.getName(), test.getQuestions().size(), test.getCreatedAt());

        return new UploadTestResponse(summary, category.getId(), category.getName(), resolution.created(),
                test.getQuestions().size(), parsed.getWarnings());
    }

    @Transactional(readOnly = true)
    public List<AdminTestSummaryDto> listTests() {
        return testRepository.findAll().stream()
                .map(test -> new AdminTestSummaryDto(
                        test.getId(),
                        test.getTitle(),
                        test.getDescription(),
                        test.getCategory().getId(),
                        test.getCategory().getName(),
                        test.getQuestions().size(),
                        test.getCreatedAt()))
                .toList();
    }
}
