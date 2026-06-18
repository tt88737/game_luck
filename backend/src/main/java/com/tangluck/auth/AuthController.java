package com.tangluck.auth;

import com.tangluck.compliance.ComplianceDocumentDto;
import com.tangluck.compliance.ComplianceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public Map<String, String> login() {
        return Map.of("status", "reserved_for_p0a");
    }

    @GetMapping("/me")
    public Map<String, String> me() {
        return Map.of("status", "reserved_for_p0a");
    }

    @GetMapping("/compliance/documents")
    public List<ComplianceDocumentDto> complianceDocuments() {
        return complianceService.activeDocuments();
    }
}
