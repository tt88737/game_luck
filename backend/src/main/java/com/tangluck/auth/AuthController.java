package com.tangluck.auth;

import com.tangluck.compliance.ComplianceDocumentDto;
import com.tangluck.compliance.ComplianceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tangluck.auth.AuthDtos.BindEmailRequest;
import static com.tangluck.auth.AuthDtos.GuestRequest;
import static com.tangluck.auth.AuthDtos.RegisterResponse;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;
    private final ComplianceService complianceService;

    public AuthController(AuthService authService, ComplianceService complianceService) {
        this.authService = authService;
        this.complianceService = complianceService;
    }

    @PostMapping("/auth/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public RegisterResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/guest")
    public RegisterResponse guest(@RequestBody GuestRequest request) {
        return authService.createGuest(request);
    }

    @PostMapping("/auth/bind-email")
    public RegisterResponse bindEmail(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BindEmailRequest request
    ) {
        return authService.bindEmail(userId, request);
    }

    @GetMapping("/me")
    public RegisterResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.me(authorization);
    }

    @GetMapping("/compliance/documents")
    public List<ComplianceDocumentDto> complianceDocuments() {
        return complianceService.activeDocuments();
    }
}
