package com.testlyflow.dto;

import jakarta.validation.Valid;

import java.util.List;

public record PrepLinksUpdateRequest(@Valid List<PrepLinkUpsertRequest> links) {
}
