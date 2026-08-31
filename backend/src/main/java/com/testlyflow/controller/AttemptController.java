package com.testlyflow.controller;

import com.testlyflow.dto.StartAttemptRequest;
import com.testlyflow.dto.StartAttemptResponse;
import com.testlyflow.dto.SubmitAttemptRequest;
import com.testlyflow.dto.SubmitAttemptResponse;
import com.testlyflow.service.AttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/tests/{testId}/attempts/start")
    public StartAttemptResponse start(@PathVariable Long testId,
                                       @Valid @RequestBody StartAttemptRequest request,
                                       HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return attemptService.startAttempt(testId, request, ip, userAgent);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public SubmitAttemptResponse submit(@PathVariable Long attemptId,
                                         @Valid @RequestBody SubmitAttemptRequest request) {
        return attemptService.submitAttempt(attemptId, request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
