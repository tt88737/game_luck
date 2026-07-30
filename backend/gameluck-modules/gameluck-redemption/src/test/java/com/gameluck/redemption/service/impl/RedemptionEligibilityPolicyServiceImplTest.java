package com.gameluck.redemption.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gameluck.redemption.domain.RedemptionEligibilityPolicy;
import com.gameluck.redemption.mapper.RedemptionEligibilityPolicyMapper;
import com.gameluck.redemption.service.IRedemptionEligibilityPolicyService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedemptionEligibilityPolicyServiceImplTest {

    @Test
    @Tag("local")
    void deniesMatchingRegionPolicy() {
        RedemptionEligibilityPolicyMapper mapper = mapperWithPolicies(List.of(
            policy("US WA deny", "SC", "US", "WA", "h5", "DENY", 100, "0")
        ));
        IRedemptionEligibilityPolicyService service = new RedemptionEligibilityPolicyServiceImpl(mapper);

        boolean eligible = service.isEligible("000000", "SC", "us", "wa", "H5");

        assertFalse(eligible);
    }

    @Test
    @Tag("local")
    void higherPriorityAllowOverridesDeny() {
        RedemptionEligibilityPolicyMapper mapper = mapperWithPolicies(List.of(
            policy("US WA deny", "SC", "US", "WA", "h5", "DENY", 100, "0"),
            policy("US WA allow test", "SC", "US", "WA", "h5", "ALLOW", 200, "0")
        ));
        IRedemptionEligibilityPolicyService service = new RedemptionEligibilityPolicyServiceImpl(mapper);

        boolean eligible = service.isEligible("000000", "SC", "US", "WA", "h5");

        assertTrue(eligible);
    }

    @Test
    @Tag("local")
    void ignoresDisabledDenyPolicy() {
        RedemptionEligibilityPolicyMapper mapper = mapperWithPolicies(List.of(
            policy("US WA disabled deny", "SC", "US", "WA", "h5", "DENY", 100, "1")
        ));
        IRedemptionEligibilityPolicyService service = new RedemptionEligibilityPolicyServiceImpl(mapper);

        boolean eligible = service.isEligible("000000", "SC", "US", "WA", "h5");

        assertTrue(eligible);
    }

    @Test
    @Tag("local")
    void ignoresExpiredDenyPolicy() {
        RedemptionEligibilityPolicy expired = policy("US WA expired deny", "SC", "US", "WA", "h5", "DENY", 100, "0");
        expired.setEndTime(new Date(System.currentTimeMillis() - 60_000));
        RedemptionEligibilityPolicyMapper mapper = mapperWithPolicies(List.of(expired));
        IRedemptionEligibilityPolicyService service = new RedemptionEligibilityPolicyServiceImpl(mapper);

        boolean eligible = service.isEligible("000000", "SC", "US", "WA", "h5");

        assertTrue(eligible);
    }

    private RedemptionEligibilityPolicyMapper mapperWithPolicies(List<RedemptionEligibilityPolicy> policies) {
        RedemptionEligibilityPolicyMapper mapper = mock(RedemptionEligibilityPolicyMapper.class);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(policies);
        return mapper;
    }

    private RedemptionEligibilityPolicy policy(String name, String currencyCode, String countryCode, String stateCode,
                                               String channel, String effect, Integer priority, String status) {
        RedemptionEligibilityPolicy policy = new RedemptionEligibilityPolicy();
        policy.setTenantId("000000");
        policy.setPolicyName(name);
        policy.setCurrencyCode(currencyCode);
        policy.setCountryCode(countryCode);
        policy.setStateCode(stateCode);
        policy.setChannel(channel);
        policy.setEffect(effect);
        policy.setPriority(priority);
        policy.setStatus(status);
        return policy;
    }
}
