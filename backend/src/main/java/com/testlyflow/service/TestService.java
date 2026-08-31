package com.testlyflow.service;

import com.testlyflow.dto.TestDetailDto;
import com.testlyflow.dto.TestSummaryDto;
import com.testlyflow.entity.Test;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestService {

    private final TestRepository testRepository;

    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    @Transactional(readOnly = true)
    public List<TestSummaryDto> listTests() {
        return testRepository.findAll().stream()
                .map(TestMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDetailDto getTestDetail(Long testId) {
        return TestMapper.toDetail(getTestOrThrow(testId));
    }

    @Transactional(readOnly = true)
    public Test getTestOrThrow(Long testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Тест с id=" + testId + " не найден"));
    }
}
