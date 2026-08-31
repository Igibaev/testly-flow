package com.testlyflow.service;

import com.testlyflow.dto.AnswerDetailDto;
import com.testlyflow.dto.AnswerSubmission;
import com.testlyflow.dto.StartAttemptRequest;
import com.testlyflow.dto.StartAttemptResponse;
import com.testlyflow.dto.SubmitAttemptRequest;
import com.testlyflow.dto.SubmitAttemptResponse;
import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.entity.AttemptStatus;
import com.testlyflow.entity.Question;
import com.testlyflow.entity.Test;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import com.testlyflow.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final TestService testService;
    private final MetricsService metricsService;

    public AttemptService(AttemptRepository attemptRepository,
                           AttemptAnswerRepository attemptAnswerRepository,
                           QuestionRepository questionRepository,
                           TestService testService,
                           MetricsService metricsService) {
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.questionRepository = questionRepository;
        this.testService = testService;
        this.metricsService = metricsService;
    }

    @Transactional
    public StartAttemptResponse startAttempt(Long testId, StartAttemptRequest request, String ipAddress, String userAgent) {
        Test test = testService.getTestOrThrow(testId);

        Attempt attempt = new Attempt();
        attempt.setTest(test);
        attempt.setFirstName(request.firstName().trim());
        attempt.setLastName(request.lastName().trim());
        attempt.setTeam(request.team().trim());
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        attempt = attemptRepository.save(attempt);
        metricsService.recordStart(testId);

        return new StartAttemptResponse(attempt.getId(), attempt.getStartedAt());
    }

    @Transactional
    public SubmitAttemptResponse submitAttempt(Long attemptId, SubmitAttemptRequest request) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Попытка с id=" + attemptId + " не найдена"));

        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new IllegalArgumentException("Попытка с id=" + attemptId + " уже завершена");
        }

        List<Question> questions = questionRepository.findByTestIdOrderByNumberAsc(attempt.getTest().getId());
        Map<Long, String> submitted = new HashMap<>();
        for (AnswerSubmission answer : request.answers()) {
            submitted.put(answer.questionId(), answer.selectedOption());
        }

        int correctCount = 0;
        List<AnswerDetailDto> details = new java.util.ArrayList<>();

        for (Question question : questions) {
            String selected = submitted.get(question.getId());
            boolean isCorrect = selected != null && selected.equalsIgnoreCase(question.getCorrectOption());
            if (isCorrect) {
                correctCount++;
            }

            AttemptAnswer attemptAnswer = new AttemptAnswer();
            attemptAnswer.setAttempt(attempt);
            attemptAnswer.setQuestion(question);
            attemptAnswer.setSelectedOption(selected != null ? selected.toUpperCase() : null);
            attemptAnswer.setCorrect(isCorrect);
            attemptAnswerRepository.save(attemptAnswer);

            details.add(new AnswerDetailDto(
                    question.getId(),
                    question.getNumber(),
                    question.getText(),
                    attemptAnswer.getSelectedOption(),
                    question.getCorrectOption(),
                    isCorrect));
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        attempt.setFinishedAt(finishedAt);
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalQuestions(questions.size());
        var scorePercent = ScoringCalculator.scorePercent(correctCount, questions.size());
        attempt.setScorePercent(scorePercent);
        attemptRepository.save(attempt);

        long durationSeconds = Duration.between(attempt.getStartedAt(), finishedAt).getSeconds();
        metricsService.recordCompletion(attempt.getTest().getId(), durationSeconds);

        details.sort((a, b) -> Integer.compare(a.number(), b.number()));

        return new SubmitAttemptResponse(attempt.getId(), correctCount, questions.size(), scorePercent, details);
    }
}
