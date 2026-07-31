package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.payment.service.IPaymentSettlementReportService;
import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("local")
class PaymentSettlementReportControllerContractTest {

    @Test
    void exposesThreeReadOnlyEndpointsWithExactPermissions() {
        Map<String, String> permissions = Map.of(
            "list", "payment:settlementReport:list",
            "batches", "payment:settlementReport:query",
            "export", "payment:settlementReport:export");

        assertThat(PaymentSettlementReportController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/payment/settlement-report");
        assertThat(PaymentSettlementReportController.class.getDeclaredMethods())
            .filteredOn(method -> permissions.containsKey(method.getName()))
            .hasSize(3)
            .allSatisfy(method -> assertThat(method.getAnnotation(SaCheckPermission.class).value())
                .containsExactly(permissions.get(method.getName())));
        assertGet("list", "/list");
        assertGet("batches", "/{date}/{providerCode}/{currencyCode}/batches");
        assertGet("export", "/export");
    }

    @Test
    void exportIsCsvAndLogsNoRequestOrResponsePayload() {
        Method export = find("export");
        GetMapping mapping = export.getAnnotation(GetMapping.class);
        Log log = export.getAnnotation(Log.class);

        assertThat(mapping.produces()).containsExactly("text/csv;charset=UTF-8");
        assertThat(log.businessType()).isEqualTo(BusinessType.EXPORT);
        assertThat(log.isSaveRequestData()).isFalse();
        assertThat(log.isSaveResponseData()).isFalse();
    }

    @Test
    void controllerDependsOnlyOnTheReadOnlyReportService() {
        assertThat(Arrays.stream(PaymentSettlementReportController.class.getDeclaredFields())
            .map(Field::getType)).containsExactly(IPaymentSettlementReportService.class);
    }

    @Test
    void exportExplicitlySetsCsvContentTypeAndDeterministicFilename() throws Exception {
        IPaymentSettlementReportService service = mock(IPaymentSettlementReportService.class);
        PaymentSettlementReportQueryBo query = new PaymentSettlementReportQueryBo();
        query.setStartDate(LocalDate.of(2026, 7, 28));
        query.setEndDate(LocalDate.of(2026, 7, 29));
        when(service.export(query)).thenReturn(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        MockHttpServletResponse response = new MockHttpServletResponse();

        new PaymentSettlementReportController(service).export(query, response);

        assertThat(response.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeader("Content-Disposition"))
            .isEqualTo("attachment; filename*=UTF-8''payment-settlement-report_2026-07-28_2026-07-29.csv");
    }

    private static void assertGet(String name, String path) {
        assertThat(find(name).getAnnotation(GetMapping.class).value()).containsExactly(path);
    }

    private static Method find(String name) {
        return Arrays.stream(PaymentSettlementReportController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name)).findFirst().orElseThrow();
    }
}
