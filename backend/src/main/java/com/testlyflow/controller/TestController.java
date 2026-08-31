package com.testlyflow.controller;

import com.testlyflow.dto.TestDetailDto;
import com.testlyflow.dto.TestSummaryDto;
import com.testlyflow.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    public List<TestSummaryDto> listTests() {
        return testService.listTests();
    }

    @GetMapping("/{id}")
    public TestDetailDto getTest(@PathVariable Long id) {
        return testService.getTestDetail(id);
    }
}
