package com.testlyflow.controller;

import com.testlyflow.dto.MetricsDto;
import com.testlyflow.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metrics")
public class AdminMetricsController {

    private final MetricsService metricsService;

    public AdminMetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public MetricsDto getMetrics(@RequestParam(required = false) Long testId) {
        return metricsService.getMetrics(testId);
    }
}
