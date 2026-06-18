package com.tangluck.auth;

import jakarta.validation.constraints.NotBlank;

public record AcceptedDocument(
        @NotBlank String documentType,
        @NotBlank String version
) {
}
