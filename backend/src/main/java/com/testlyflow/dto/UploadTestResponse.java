package com.testlyflow.dto;

import java.util.List;

public record UploadTestResponse(AdminTestSummaryDto test, List<String> warnings) {
}
