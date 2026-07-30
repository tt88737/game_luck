package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportPageVo;
import com.gameluck.payment.service.IPaymentSettlementReportService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class PaymentSettlementReportControllerContractTest {

    @Test
    void exposesExactReadOnlyEndpointContract() throws Exception {
        assertThat(PaymentSettlementReportController.class.getAnnotation(RequestMapping.class).value())
            .containsExactly("/payment/settlement-report");

        Method list = method("list", PaymentSettlementReportQueryBo.class, PageQuery.class);
        assertGet(list, "/list", "payment:settlementReport:list");
        assertThat(list.getParameters()[0].getAnnotation(Validated.class)).isNotNull();
        assertGenericReturn(list, R.class, PaymentSettlementReportPageVo.class);

        Method batches = method("batches", LocalDate.class, String.class, String.class);
        assertGet(batches, "/{date}/{providerCode}/{currencyCode}/batches", "payment:settlementReport:query");
        assertThat(batches.getGenericReturnType().getTypeName()).isEqualTo(
            "com.gameluck.common.core.domain.R<java.util.List<com.gameluck.payment.domain.vo.PaymentSettlementBatchVo>>");

        Method export = method("export", PaymentSettlementReportQueryBo.class, jakarta.servlet.http.HttpServletResponse.class);
        assertGet(export, "/export", "payment:settlementReport:export");
        assertThat(export.getAnnotation(GetMapping.class).produces()).containsExactly("text/csv;charset=UTF-8");
        assertThat(export.getParameters()[0].getAnnotation(Validated.class)).isNotNull();
    }

    @Test
    void exportHasRequiredAuditPolicyAndOnlyReadServiceDependency() throws Exception {
        Method export = method("export", PaymentSettlementReportQueryBo.class, jakarta.servlet.http.HttpServletResponse.class);
        Log log = export.getAnnotation(Log.class);
        assertThat(log.title()).isEqualTo("Payment settlement report export");
        assertThat(log.businessType()).isEqualTo(BusinessType.EXPORT);
        assertThat(log.isSaveRequestData()).isFalse();
        assertThat(log.isSaveResponseData()).isFalse();

        assertThat(PaymentSettlementReportController.class.getDeclaredConstructors()).singleElement()
            .satisfies(constructor -> assertThat(constructor.getParameterTypes())
                .containsExactly(IPaymentSettlementReportService.class));
        assertThat(PaymentSettlementReportController.class.getDeclaredFields())
            .filteredOn(field -> !Modifier.isStatic(field.getModifiers()))
            .singleElement().satisfies(field -> assertThat(field.getType())
                .isEqualTo(IPaymentSettlementReportService.class));
    }

    @Test
    void delegatesQueriesAndWritesExportBytesWithSafeDeterministicHeaders() throws Exception {
        IPaymentSettlementReportService service = mock(IPaymentSettlementReportService.class);
        PaymentSettlementReportController controller = new PaymentSettlementReportController(service);
        PaymentSettlementReportQueryBo query = new PaymentSettlementReportQueryBo();
        PageQuery pageQuery = new PageQuery(1, 10);
        PaymentSettlementReportPageVo page = new PaymentSettlementReportPageVo();
        when(service.queryPage(query, pageQuery)).thenReturn(page);

        assertThat(controller.list(query, pageQuery).getData()).isSameAs(page);
        verify(service).queryPage(query, pageQuery);

        LocalDate date = LocalDate.of(2026, 7, 29);
        List<PaymentSettlementBatchVo> batches = List.of(new PaymentSettlementBatchVo());
        when(service.queryBatches(date, "SIMULATED", "USD")).thenReturn(batches);
        assertThat(controller.batches(date, "SIMULATED", "USD").getData()).isSameAs(batches);
        verify(service).queryBatches(date, "SIMULATED", "USD");

        byte[] csv = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', ',', 'b'};
        when(service.export(query)).thenReturn(csv);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(query, response);

        verify(service).export(query);
        assertThat(response.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeader("Content-Disposition")).isEqualTo(
            "attachment; filename=\"payment-settlement-report.csv\"; filename*=UTF-8''payment-settlement-report.csv");
        assertThat(response.getContentAsByteArray()).containsExactly(csv);
    }

    private static Method method(String name, Class<?>... parameterTypes) throws Exception {
        return PaymentSettlementReportController.class.getDeclaredMethod(name, parameterTypes);
    }

    private static void assertGet(Method method, String path, String permission) {
        assertThat(method.getAnnotation(GetMapping.class)).isNotNull();
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(path);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
    }

    private static void assertGenericReturn(Method method, Class<?> rawType, Class<?> argumentType) {
        assertThat(method.getGenericReturnType()).isInstanceOfSatisfying(ParameterizedType.class, type -> {
            assertThat(type.getRawType()).isEqualTo(rawType);
            assertThat(type.getActualTypeArguments()[0]).isEqualTo(argumentType);
        });
    }
}
