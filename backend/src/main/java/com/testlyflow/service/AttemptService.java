package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.*;
import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.entity.AttemptStatus;
import com.testlyflow.entity.Category;
import com.testlyflow.entity.Question;
import com.testlyflow.exception.ConflictException;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import com.testlyflow.repository.CategoryRepository;
import com.testlyflow.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final FeedbackService feedbackService;
    private final TestlyProperties properties;

    public AttemptService(AttemptRepository attemptRepository,
                           AttemptAnswerRepository attemptAnswerRepository,
                           QuestionRepository questionRepository,
                           CategoryRepository categoryRepository,
                           FeedbackService feedbackService,
                           TestlyProperties properties) {
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.feedbackService = feedbackService;
        this.properties = properties;
    }

    @Transactional
    public StartAttemptResponse startAttempt(StartAttemptRequest request, String ipAddress, String userAgent) {
        List<Category> categories = categoryRepository.findAllWithQuestionsOrdered();
        if (categories.isEmpty()) {
            throw new ConflictException("Тест ещё не наполнен вопросами");
        }

        List<Long> orderedQuestionIds = new ArrayList<>();
        for (Category category : categories) {
            int min = category.getQuestionsMin() != null ? category.getQuestionsMin()
                    : properties.getSampling().getQuestionsPerCategoryMin();
            int max = category.getQuestionsMax() != null ? category.getQuestionsMax()
                    : properties.getSampling().getQuestionsPerCategoryMax();
            if (max < min) {
                max = min;
            }
            int n = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);

            List<Long> sample = questionRepository.sampleRandomIdsByCategory(category.getId(), n);
            orderedQuestionIds.addAll(sample);
        }

        if (properties.getSampling().isShuffleQuestions()) {
            Collections.shuffle(orderedQuestionIds);
        }

        List<Question> questions = questionRepository.findAllById(orderedQuestionIds);
        Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        Attempt attempt = new Attempt();
        attempt.setFirstName(request.firstName().trim());
        attempt.setLastName(request.lastName().trim());
        attempt.setTeam(request.team().trim());
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt = attemptRepository.save(attempt);

        List<AttemptQuestionDto> questionDtos = new ArrayList<>();
        Set<Long> categoriesInAttempt = new java.util.HashSet<>();
        int displayNumber = 1;
        for (Long questionId : orderedQuestionIds) {
            Question question = byId.get(questionId);
            if (question == null) {
                continue;
            }

            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setDisplayNumber(displayNumber);
            answer.setSelectedOption(null);
            answer.setCorrect(false);
            attemptAnswerRepository.save(answer);

            questionDtos.add(new AttemptQuestionDto(
                    question.getId(),
                    displayNumber,
                    question.getCategory().getId(),
                    question.getCategory().getName(),
                    question.getText(),
                    question.getOptions().stream()
                            .map(o -> new QuestionOptionDto(o.getOptionLetter(), o.getText()))
                            .toList()));

            categoriesInAttempt.add(question.getCategory().getId());
            displayNumber++;
        }

        return new StartAttemptResponse(attempt.getId(), attempt.getStartedAt(), questionDtos,
                questionDtos.size(), categoriesInAttempt.size());
    }

    @Transactional(readOnly = true)
    public AttemptStateDto getAttemptState(Long attemptId) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ConflictException("Попытка с id=" + attemptId + " уже завершена");
        }

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptIdOrderByDisplayNumberAsc(attemptId);

        List<AttemptQuestionDto> questionDtos = answers.stream()
                .map(a -> new AttemptQuestionDto(
                        a.getQuestion().getId(),
                        a.getDisplayNumber(),
                        a.getQuestion().getCategory().getId(),
                        a.getQuestion().getCategory().getName(),
                        a.getQuestion().getText(),
                        a.getQuestion().getOptions().stream()
                                .map(o -> new QuestionOptionDto(o.getOptionLetter(), o.getText()))
                                .toList()))
                .toList();

        List<SavedAnswerDto> savedAnswers = answers.stream()
                .filter(a -> a.getSelectedOption() != null || a.getTimeSpentMs() > 0)
                .map(a -> new SavedAnswerDto(a.getQuestion().getId(), a.getSelectedOption(), a.getTimeSpentMs()))
                .toList();

        long categoriesCount = answers.stream().map(a -> a.getQuestion().getCategory().getId()).distinct().count();

        return new AttemptStateDto(attempt.getId(), attempt.getStartedAt(), questionDtos,
                questionDtos.size(), (int) categoriesCount, savedAnswers);
    }

    @Transactional
    public void updateAnswer(Long attemptId, Long questionId, AnswerUpdateRequest request) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ConflictException("Попытка с id=" + attemptId + " уже завершена");
        }

        AttemptAnswer answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Вопрос с id=" + questionId + " не входит в состав попытки id=" + attemptId));

        String selected = normalizeSelectedOption(answer.getQuestion(), request.selectedOption());
        answer.setSelectedOption(selected);
        answer.setCorrect(selected != null && selected.equalsIgnoreCase(answer.getQuestion().getCorrectOption()));
        answer.setAnsweredAt(selected != null ? OffsetDateTime.now() : null);
        answer.setTimeSpentMs(sanitizeTimeSpentMs(request.timeSpentMs()));
        answer.setVisitsCount(answer.getVisitsCount() + 1);

        attemptAnswerRepository.save(answer);
    }

    private String normalizeSelectedOption(Question question, String selectedOption) {
        if (selectedOption == null || selectedOption.isBlank()) {
            return null;
        }
        String upper = selectedOption.trim().toUpperCase();
        boolean valid = question.getOptions().stream()
                .anyMatch(o -> o.getOptionLetter().equalsIgnoreCase(upper));
        if (!valid) {
            throw new IllegalArgumentException(
                    "\"" + selectedOption + "\" не является вариантом ответа вопроса id=" + question.getId());
        }
        return upper;
    }

    private long sanitizeTimeSpentMs(Long raw) {
        long value = raw != null ? raw : 0L;
        if (value < 0) {
            return 0L;
        }
        long max = properties.getTiming().getMaxTimeSpentMs();
        return Math.min(value, max);
    }

    @Transactional
    public SubmitAttemptResponse submitAttempt(Long attemptId, SubmitAttemptRequest request) {
        Attempt attempt = getAttemptOrThrow(attemptId);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new IllegalArgumentException("Попытка с id=" + attemptId + " уже завершена");
        }

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptIdOrderByDisplayNumberAsc(attemptId);
        Map<Long, AttemptAnswer> byQuestionId = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a, HashMap::new));

        for (AnswerSubmission submission : request.answers()) {
            AttemptAnswer answer = byQuestionId.get(submission.questionId());
            if (answer == null) {
                continue; // questionId outside this attempt's composition -- ignored, logged below
            }
            String selected = normalizeSelectedOptionSafely(answer.getQuestion(), submission.selectedOption());
            answer.setSelectedOption(selected);
            answer.setCorrect(selected != null && selected.equalsIgnoreCase(answer.getQuestion().getCorrectOption()));
            if (selected != null) {
                answer.setAnsweredAt(OffsetDateTime.now());
            }
        }
        for (TimingSubmission timing : request.timings()) {
            AttemptAnswer answer = byQuestionId.get(timing.questionId());
            if (answer == null) {
                continue;
            }
            answer.setTimeSpentMs(sanitizeTimeSpentMs(timing.timeSpentMs()));
        }
        attemptAnswerRepository.saveAll(answers);

        long ignoredQuestionIds = request.answers().stream()
                .map(AnswerSubmission::questionId)
                .filter(id -> !byQuestionId.containsKey(id))
                .count();
        if (ignoredQuestionIds > 0) {
            org.slf4j.LoggerFactory.getLogger(AttemptService.class)
                    .warn("submit for attempt {}: ignored {} questionId(s) outside its composition",
                            attemptId, ignoredQuestionIds);
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        int correctCount = (int) answers.stream().filter(AttemptAnswer::isCorrect).count();
        int totalQuestions = answers.size();
        var scorePercent = ScoringCalculator.scorePercent(correctCount, totalQuestions);

        long totalTimeSpentMs = answers.stream().mapToLong(AttemptAnswer::getTimeSpentMs).sum();
        long attemptDurationMs = Duration.between(attempt.getStartedAt(), finishedAt).toMillis();
        double overrunRatio = properties.getTiming().getSuspiciousOverrunRatio();
        boolean suspicious = attemptDurationMs > 0
                && totalTimeSpentMs > attemptDurationMs * (1.0 + overrunRatio);

        attempt.setFinishedAt(finishedAt);
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setScorePercent(scorePercent);
        attempt.setTimingSuspicious(suspicious);
        attemptRepository.save(attempt);

        // Public-facing (never carries the correct option, only whether the taker's own pick
        // was right -- see PublicAnswerDetailDto).
        List<PublicAnswerDetailDto> details = answers.stream()
                .map(a -> new PublicAnswerDetailDto(
                        a.getQuestion().getId(),
                        a.getQuestion().getNumber(),
                        a.getQuestion().getText(),
                        a.getQuestion().getCategory().getId(),
                        a.getQuestion().getCategory().getName(),
                        a.getSelectedOption(),
                        a.isCorrect(),
                        a.getTimeSpentMs()))
                .sorted((x, y) -> Integer.compare(x.number(), y.number()))
                .toList();

        FeedbackService.Feedback feedback = feedbackService.buildFeedback(answers, scorePercent);

        return new SubmitAttemptResponse(attempt.getId(), correctCount, totalQuestions, scorePercent,
                feedback.tier().name(), feedback.headline(), feedback.message(), feedback.focusAreas(), details);
    }

    private String normalizeSelectedOptionSafely(Question question, String selectedOption) {
        try {
            return normalizeSelectedOption(question, selectedOption);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Attempt getAttemptOrThrow(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Попытка с id=" + attemptId + " не найдена"));
    }
}
