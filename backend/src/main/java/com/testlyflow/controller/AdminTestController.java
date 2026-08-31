package com.testlyflow.controller;

import com.testlyflow.dto.AdminTestSummaryDto;
import com.testlyflow.dto.PrepLinkUpsertRequest;
import com.testlyflow.dto.PrepLinksUpdateRequest;
import com.testlyflow.dto.UploadTestResponse;
import com.testlyflow.service.AdminTestService;
import jakarta.validation.Valid;
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
                                          @RequestParam(value = "prepLinkTitles", required = false) List<String> prepLinkTitles,
                                          @RequestParam(value = "prepLinkUrls", required = false) List<String> prepLinkUrls) {
        List<PrepLinkUpsertRequest> prepLinks = buildPrepLinks(prepLinkTitles, prepLinkUrls);
        return adminTestService.uploadTest(file, title, prepLinks);
    }

    @PutMapping("/{id}/prep-links")
    public void updatePrepLinks(@PathVariable Long id, @Valid @RequestBody PrepLinksUpdateRequest request) {
        adminTestService.updatePrepLinks(id, request.links());
    }

    private List<PrepLinkUpsertRequest> buildPrepLinks(List<String> titles, List<String> urls) {
        if (titles == null || urls == null || titles.isEmpty()) {
            return List.of();
        }
        if (titles.size() != urls.size()) {
            throw new IllegalArgumentException("Количество заголовков и URL подготовительных ссылок не совпадает");
        }
        return java.util.stream.IntStream.range(0, titles.size())
                .mapToObj(i -> new PrepLinkUpsertRequest(titles.get(i), urls.get(i)))
                .toList();
    }
}
