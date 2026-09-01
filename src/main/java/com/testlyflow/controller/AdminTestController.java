package com.testlyflow.controller;

import com.testlyflow.dto.AdminTestSummaryDto;
import com.testlyflow.dto.UploadTestResponse;
import com.testlyflow.service.AdminTestService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tests")
public class AdminTestController {

    private final AdminTestService adminTestService;

    public AdminTestController(AdminTestService adminTestService) {
        this.adminTestService = adminTestService;
    }

    @GetMapping
    public List<AdminTestSummaryDto> listTests() {
        return adminTestService.listTests();
    }

    @PostMapping(consumes = "multipart/form-data")
    public UploadTestResponse uploadTest(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "title", required = false) String title,
                                          @RequestParam(value = "categoryId", required = false) Long categoryId,
                                          @RequestParam(value = "newCategoryName", required = false) String newCategoryName,
                                          @RequestParam(value = "newCategoryDescription", required = false) String newCategoryDescription,
                                          @RequestParam(value = "newCategoryColor", required = false) String newCategoryColor) {
        try {
            return adminTestService.uploadTest(file.getBytes(), title, categoryId, newCategoryName,
                    newCategoryDescription, newCategoryColor);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Не удалось прочитать содержимое файла", e);
        }
    }
}
