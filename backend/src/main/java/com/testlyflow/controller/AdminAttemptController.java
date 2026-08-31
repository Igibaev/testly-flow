package com.testlyflow.controller;

import com.testlyflow.dto.AdminAttemptDetailDto;
import com.testlyflow.dto.AdminAttemptSummaryDto;
import com.testlyflow.dto.PageResponse;
import com.testlyflow.service.AdminAttemptService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/attempts")
public class AdminAttemptController {

    private final AdminAttemptService adminAttemptService;

    public AdminAttemptController(AdminAttemptService adminAttemptService) {
        this.adminAttemptService = adminAttemptService;
    }

    @GetMapping
    public PageResponse<AdminAttemptSummaryDto> search(@RequestParam(required = false) Long testId,
                                                         @RequestParam(required = false) String team,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return adminAttemptService.search(testId, team, page, size);
    }

    @GetMapping("/{id}")
    public AdminAttemptDetailDto getDetail(@PathVariable Long id) {
        return adminAttemptService.getDetail(id);
    }
}
