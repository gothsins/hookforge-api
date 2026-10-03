package io.github.gothsins.hookforge.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateEventRequest(

        @NotBlank
        @Size(max = 120)
        String type,

        @NotNull
        Map<String, Object> payload
) {
}