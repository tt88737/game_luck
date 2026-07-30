package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutRowVo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class PaymentSettlementPayoutControllerContractTest {
    @Test
    void exposesEightRoutesWithSixExactPermissions() {
        Map<String, String> expected = Map.of(
            "list", "payment:settlementPayout:list", "detail", "payment:settlementPayout:query",
            "create", "payment:settlementPayout:create", "edit", "payment:settlementPayout:create",
            "submit", "payment:settlementPayout:submit", "approve", "payment:settlementPayout:approve",
            "reject", "payment:settlementPayout:approve", "cancel", "payment:settlementPayout:cancel");
        assertThat(PaymentSettlementPayoutController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/payment/settlement-payout");
        assertThat(PaymentSettlementPayoutController.class.getDeclaredMethods())
            .filteredOn(method -> expected.containsKey(method.getName())).hasSize(8)
            .allSatisfy(method -> assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly(expected.get(method.getName())));
    }

    @Test
    void mutationsUseSanitizedOperationLogs() {
        for (String name : new String[]{"create", "edit", "submit", "approve", "reject", "cancel"}) {
            Log log = find(name).getAnnotation(Log.class);
            assertThat(log).as(name).isNotNull();
            assertThat(log.isSaveRequestData()).isFalse();
            assertThat(log.isSaveResponseData()).isFalse();
        }
    }

    @Test
    void listAndCommandsKeepBoundaryContracts() throws Exception {
        assertThat(find("list").getReturnType()).isEqualTo(TableDataInfo.class);
        assertThat(PaymentSettlementPayoutRowVo.class.getMethod("getId").getReturnType()).isEqualTo(String.class);
        assertThat(PaymentSettlementPayoutRowVo.class.getMethod("getPayoutAmount").getReturnType()).isEqualTo(String.class);
        assertThat(PaymentSettlementPayoutCommandBo.class.getDeclaredField("version").getAnnotation(NotNull.class)).isNotNull();
        assertThat(PaymentSettlementPayoutCommandBo.class.getDeclaredField("reason").getAnnotation(NotBlank.class)).isNotNull();
    }

    private static Method find(String name) {
        return java.util.Arrays.stream(PaymentSettlementPayoutController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name)).findFirst().orElseThrow();
    }
}
