package com.tangluck.auth;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.compliance.ComplianceRegionRepository;
import com.tangluck.wallet.WalletAccount;
import com.tangluck.wallet.WalletAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Set;

import static com.tangluck.auth.AuthDtos.RegisterResponse;
import static com.tangluck.auth.AuthDtos.UserDto;
import static com.tangluck.auth.AuthDtos.WalletDto;

@Service
public class AuthService {
    private static final Set<String> REQUIRED_DOCUMENT_TYPES = Set.of("terms", "sweepstakes_rules", "privacy");

    private final UserRepository userRepository;
    private final UserConsentLogRepository consentLogRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final ComplianceRegionRepository complianceRegionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            UserConsentLogRepository consentLogRepository,
            WalletAccountRepository walletAccountRepository,
            ComplianceRegionRepository complianceRegionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.consentLogRepository = consentLogRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.complianceRegionRepository = complianceRegionRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS, "Email already exists.", Map.of("email", request.email()));
        }

        if (Period.between(request.birthDate(), LocalDate.now(clock)).getYears() < 18) {
            throw new BusinessException(ErrorCode.AGE_NOT_ALLOWED, "User must be at least 18 years old.");
        }

        var acceptedTypes = request.acceptedDocuments().stream()
                .map(AcceptedDocument::documentType)
                .collect(java.util.stream.Collectors.toSet());
        if (!acceptedTypes.containsAll(REQUIRED_DOCUMENT_TYPES)) {
            throw new BusinessException(ErrorCode.CONSENT_REQUIRED, "Required compliance documents must be accepted.");
        }

        var region = complianceRegionRepository
                .findByCountryCodeAndStateCode(request.countryCode(), request.stateCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_BLOCKED, "This region is not configured."));
        if (!region.isRegistrationAllowed() || !"active".equals(region.getStatus())) {
            throw new BusinessException(
                    ErrorCode.REGION_BLOCKED,
                    "This feature is not available in your region.",
                    Map.of("state_code", request.stateCode(), "feature", "registration")
            );
        }

        var now = clock.instant();
        var user = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.birthDate(),
                request.countryCode(),
                request.stateCode(),
                request.deviceId(),
                now
        ));

        request.acceptedDocuments().forEach(document ->
                consentLogRepository.save(new UserConsentLog(
                        user.getId(),
                        document.documentType(),
                        document.version(),
                        now,
                        request.deviceId()
                ))
        );

        var gcWallet = walletAccountRepository.save(new WalletAccount(user.getId(), "GC", now));
        var scWallet = walletAccountRepository.save(new WalletAccount(user.getId(), "SC", now));

        return new RegisterResponse(
                new UserDto(user.getId(), user.getEmail(), user.getCountryCode(), user.getStateCode(), user.getRiskLevel(), user.getStatus()),
                new WalletDto(gcWallet.getBalance(), scWallet.getBalance(), scWallet.getFrozenBalance()),
                "local-user-" + user.getId()
        );
    }
}
