package com.testlyflow.service;

import com.testlyflow.dto.AdminAttemptDetailDto;
import com.testlyflow.dto.AdminAttemptSummaryDto;
import com.testlyflow.dto.AnswerDetailDto;
import com.testlyflow.dto.PageResponse;
import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminAttemptService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public AdminAttemptService(AttemptRepository attemptRepository, AttemptAnswerRepository attemptAnswerRepository) {
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAttemptSummaryDto> search(Long testId, String team, int page, int size) {
        Page<Attempt> result = attemptRepository.search(testId, team, PageRequest.of(page, size));
        List<AdminAttemptSummaryDto> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminAttemptDetailDto getDetail(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Попытка с id=" + attemptId + " не найдена"));

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        List<AnswerDetailDto> details = answers.stream()
                .map(a -> new AnswerDetailDto(
                        a.getQuestion().getId(),
                        a.getQuestion().getNumber(),
                        a.getQuestion().getText(),
                        a.getSelectedOption(),
                        a.getQuestion().getCorrectOption(),
                        a.isCorrect()))
                .sorted(Comparator.comparingInt(AnswerDetailDto::number))
                .toList();

        return new AdminAttemptDetailDto(toSummary(attempt), details);
    }

    private AdminAttemptSummaryDto toSummary(Attempt attempt) {
        return new AdminAttemptSummaryDto(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getTitle(),
                attempt.getFirstName(),
                attempt.getLastName(),
                attempt.getTeam(),
                attempt.getIpAddress(),
                attempt.getUserAgent(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getStatus().name(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                attempt.getScorePercent());
    }
}
