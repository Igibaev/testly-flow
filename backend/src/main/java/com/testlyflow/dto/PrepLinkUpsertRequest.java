package com.testlyflow.dto;

import jakarta.validation.constraints.NotBlank;

public record PrepLinkUpsertRequest(
        @NotBlank(message = "укажите заголовок ссылки") String title,
        @NotBlank(message = "укажите url ссылки") String url
) {
}
