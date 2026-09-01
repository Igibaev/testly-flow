package com.testlyflow.dto;

import java.util.List;

public record UploadTestResponse(
        AdminTestSummaryDto test,
        Long categoryId,
        String categoryName,
        boolean categoryCreated,
        int questionsAdded,
        List<String> warnings
) {
}
