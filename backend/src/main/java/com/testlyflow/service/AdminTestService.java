package com.testlyflow.service;

import com.testlyflow.dto.AdminTestSummaryDto;
import com.testlyflow.dto.PrepLinkUpsertRequest;
import com.testlyflow.dto.UploadTestResponse;
import com.testlyflow.entity.PrepLink;
import com.testlyflow.entity.Question;
import com.testlyflow.entity.QuestionOption;
import com.testlyflow.entity.Test;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.parser.MarkdownTestParser;
import com.testlyflow.parser.ParsedQuestion;
import com.testlyflow.parser.ParsedTestResult;
import com.testlyflow.repository.AttemptRepository;
import com.testlyflow.repository.PrepLinkRepository;
import com.testlyflow.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AdminTestService {

    private final TestRepository testRepository;
    private final PrepLinkRepository prepLinkRepository;
    private final AttemptRepository attemptRepository;
    private final MarkdownTestParser parser;

    public AdminTestService(TestRepository testRepository,
                             PrepLinkRepository prepLinkRepository,
                             AttemptRepository attemptRepository,
                             MarkdownTestParser parser) {
        this.testRepository = testRepository;
        this.prepLinkRepository = prepLinkRepository;
        this.attemptRepository = attemptRepository;
        this.parser = parser;
    }

    @Transactional
    public UploadTestResponse uploadTest(MultipartFile file, String titleParam, List<PrepLinkUpsertRequest> prepLinks) {
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать содержимое файла", e);
        }

        ParsedTestResult parsed = parser.parse(content);

        String title = (titleParam != null && !titleParam.isBlank()) ? titleParam.trim() : parsed.getTitleFromFile();
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Не указано название теста: передайте параметр title или добавьте заголовок \"# ...\" в начало файла");
        }

        Test test = new Test();
        test.setTitle(title);

        for (ParsedQuestion pq : parsed.getQuestions()) {
            Question question = new Question();
            question.setTest(test);
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

        if (prepLinks != null) {
            int order = 0;
            for (PrepLinkUpsertRequest linkRequest : prepLinks) {
                PrepLink link = new PrepLink();
                link.setTest(test);
                link.setTitle(linkRequest.title());
                link.setUrl(linkRequest.url());
                link.setSortOrder(order++);
                test.getPrepLinks().add(link);
            }
        }

        test = testRepository.save(test);

        AdminTestSummaryDto summary = new AdminTestSummaryDto(
                test.getId(), test.getTitle(), test.getDescription(),
                test.getQuestions().size(), 0L, test.getCreatedAt());

        return new UploadTestResponse(summary, parsed.getWarnings());
    }

    @Transactional(readOnly = true)
    public List<AdminTestSummaryDto> listTests() {
        return testRepository.findAll().stream()
                .map(test -> new AdminTestSummaryDto(
                        test.getId(),
                        test.getTitle(),
                        test.getDescription(),
                        test.getQuestions().size(),
                        attemptRepository.countByTestId(test.getId()),
                        test.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void updatePrepLinks(Long testId, List<PrepLinkUpsertRequest> links) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Тест с id=" + testId + " не найден"));

        prepLinkRepository.deleteByTestId(testId);

        int order = 0;
        for (PrepLinkUpsertRequest linkRequest : links) {
            PrepLink link = new PrepLink();
            link.setTest(test);
            link.setTitle(linkRequest.title());
            link.setUrl(linkRequest.url());
            link.setSortOrder(order++);
            prepLinkRepository.save(link);
        }
    }
}
