package com.testlyflow.dto;

import jakarta.validation.constraints.NotBlank;

public record StartAttemptRequest(
        @NotBlank(message = "укажите имя") String firstName,
        @NotBlank(message = "укажите фамилию") String lastName,
        @NotBlank(message = "укажите название команды") String team
) {
}
