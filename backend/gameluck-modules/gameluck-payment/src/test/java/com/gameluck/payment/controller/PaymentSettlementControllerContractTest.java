package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.payment.domain.bo.PaymentSettlementCloseBo;
import com.gameluck.payment.domain.bo.PaymentSettlementCreateBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementDetailVo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class PaymentSettlementControllerContractTest {

    @Test
    void exposesApprovedSixEndpointSurfaceWithExactPermissions() {
        Map<String, String> expected = Map.of(
            "list", "payment:settlement:list",
            "create", "payment:settlement:create",
            "detail", "payment:settlement:query",
            "items", "payment:settlement:query",
            "calculate", "payment:settlement:calculate",
            "close", "payment:settlement:close");

        assertThat(PaymentSettlementController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/payment/settlement");
        assertThat(PaymentSettlementController.class.getDeclaredMethods())
            .filteredOn(method -> expected.containsKey(method.getName()))
            .hasSize(6)
            .allSatisfy(method -> assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly(expected.get(method.getName())));
    }

    @Test
    void mutationsAreLoggedAndSensitiveBodiesAreNotPersisted() {
        for (String name : new String[]{"create", "calculate", "close"}) {
            Log log = find(name).getAnnotation(Log.class);
            assertThat(log).as(name).isNotNull();
            assertThat(log.isSaveRequestData()).as(name + " request").isFalse();
            assertThat(log.isSaveResponseData()).as(name + " response").isFalse();
        }
    }

    @Test
    void createAndCloseBodiesUseControllerValidation() throws Exception {
        assertValidatedBody("create", PaymentSettlementCreateBo.class);
        assertValidatedBody("close", PaymentSettlementCloseBo.class);
        assertThat(PaymentSettlementCloseBo.class.getDeclaredField("version")
            .getAnnotation(NotNull.class)).isNotNull();
        assertThat(PaymentSettlementCloseBo.class.getDeclaredField("remark")
            .getAnnotation(NotBlank.class)).isNotNull();
    }

    @Test
    void identifiersAndMoneyRemainStringSafeAtTheAdminBoundary() throws Exception {
        assertThat(PaymentSettlementBatchVo.class.getMethod("getId").getReturnType()).isEqualTo(String.class);
        assertThat(PaymentSettlementDetailVo.class.getMethod("getId").getReturnType()).isEqualTo(String.class);
        for (String getter : new String[]{"getGrossPayment", "getRefundAmount", "getChargebackAmount",
            "getTotalFee", "getNetSettlement"}) {
            assertThat(PaymentSettlementBatchVo.class.getMethod(getter).getReturnType()).as(getter)
                .isEqualTo(String.class);
        }
    }

    @Test
    void sqlMetadataDefinesOnePageFivePermissionsAndBilingualIdempotentDictionaries() throws Exception {
        String walletSql = read("backend/script/sql/gameluck_wallet.sql");
        String englishSql = read("backend/script/sql/gameluck_platform_dict.sql");

        assertThat(walletSql).contains("(2033,'支付结算',1900,7,'payment-settlement'")
            .contains("payment:settlement:list", "payment:settlement:query", "payment:settlement:create",
                "payment:settlement:calculate", "payment:settlement:close")
            .contains("DELETE FROM sys_menu WHERE menu_id IN (20331,20332,20333,20334,2033)")
            .contains("gl_payment_settlement_batch_status", "gl_payment_settlement_action_type");
        assertThat(englishSql).contains("Payment Settlement Batch Status", "Payment Settlement Action Type",
                "Calculation Failed", "Close Rejected")
            .contains("WHERE NOT EXISTS")
            .contains("gl_payment_settlement_batch_status", "gl_payment_settlement_action_type");
    }

    @Test
    void allMessageBundlesContainStableSettlementFailures() throws Exception {
        for (String file : new String[]{"messages.properties", "messages_en_US.properties", "messages_zh_CN.properties"}) {
            String messages = read("backend/gameluck-admin/src/main/resources/i18n/" + file);
            assertThat(messages).as(file).contains(
                "payment.settlement.overlap=", "payment.settlement.window.invalid=",
                "payment.settlement.fee.invalid=", "payment.settlement.calculate.stateConflict=",
                "payment.settlement.calculate.failed=", "payment.settlement.close.missingReconciliation=",
                "payment.settlement.close.openIssues=", "payment.settlement.close.stateConflict=");
        }
    }

    private static void assertValidatedBody(String methodName, Class<?> bodyType) {
        Parameter body = Arrays.stream(find(methodName).getParameters())
            .filter(parameter -> parameter.getType() == bodyType).findFirst().orElseThrow();
        assertThat(body.getAnnotation(RequestBody.class)).as(methodName + " request body").isNotNull();
        assertThat(body.getAnnotation(Validated.class)).as(methodName + " validation").isNotNull();
    }

    private static Method find(String name) {
        return Arrays.stream(PaymentSettlementController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name)).findFirst().orElseThrow();
    }

    private static String read(String relativePath) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve(relativePath))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Repository root not found for " + relativePath);
        return Files.readString(current.resolve(relativePath));
    }
}
