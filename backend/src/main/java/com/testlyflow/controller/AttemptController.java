package com.testlyflow.controller;

import com.testlyflow.dto.*;
import com.testlyflow.service.AttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/start")
    public StartAttemptResponse start(@Valid @RequestBody StartAttemptRequest request,
                                       HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return attemptService.startAttempt(request, ip, userAgent);
    }

    @GetMapping("/{attemptId}")
    public AttemptStateDto getState(@PathVariable Long attemptId) {
        return attemptService.getAttemptState(attemptId);
    }

    @PutMapping("/{attemptId}/answers/{questionId}")
    public ResponseEntity<Void> updateAnswer(@PathVariable Long attemptId,
                                              @PathVariable Long questionId,
                                              @RequestBody AnswerUpdateRequest request) {
        attemptService.updateAnswer(attemptId, questionId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{attemptId}/submit")
    public SubmitAttemptResponse submit(@PathVariable Long attemptId,
                                         @RequestBody(required = false) SubmitAttemptRequest request) {
        return attemptService.submitAttempt(attemptId, request != null ? request : new SubmitAttemptRequest(null, null));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
