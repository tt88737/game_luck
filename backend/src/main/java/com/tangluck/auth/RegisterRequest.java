package com.tangluck.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotNull LocalDate birthDate,
        @NotBlank String countryCode,
        @NotBlank String stateCode,
        @NotEmpty List<@Valid AcceptedDocument> acceptedDocuments,
        String utmSource,
        String deviceId
) {
    public RegisterRequest withBirthDate(LocalDate newBirthDate) {
        return new RegisterRequest(email, password, newBirthDate, countryCode, stateCode, acceptedDocuments, utmSource, deviceId);
    }

    public RegisterRequest withAcceptedDocuments(List<AcceptedDocument> documents) {
        return new RegisterRequest(email, password, birthDate, countryCode, stateCode, documents, utmSource, deviceId);
    }
}
