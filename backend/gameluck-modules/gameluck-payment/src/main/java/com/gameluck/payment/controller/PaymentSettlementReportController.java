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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/settlement-report")
public class PaymentSettlementReportController {
    private static final String CSV_CONTENT_TYPE = "text/csv;charset=UTF-8";
    private static final String CSV_FILE_NAME = "payment-settlement-report.csv";

    private final IPaymentSettlementReportService reportService;

    @SaCheckPermission("payment:settlementReport:list")
    @GetMapping("/list")
    public R<PaymentSettlementReportPageVo> list(@Validated PaymentSettlementReportQueryBo bo,
            PageQuery pageQuery) {
        return R.ok(reportService.queryPage(bo, pageQuery));
    }

    @SaCheckPermission("payment:settlementReport:query")
    @GetMapping("/{date}/{providerCode}/{currencyCode}/batches")
    public R<List<PaymentSettlementBatchVo>> batches(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable("providerCode") String providerCode,
            @PathVariable("currencyCode") String currencyCode) {
        return R.ok(reportService.queryBatches(date, providerCode, currencyCode));
    }

    @SaCheckPermission("payment:settlementReport:export")
    @Log(title = "Payment settlement report export", businessType = BusinessType.EXPORT,
        isSaveRequestData = false, isSaveResponseData = false)
    @GetMapping(value = "/export", produces = CSV_CONTENT_TYPE)
    public void export(@Validated PaymentSettlementReportQueryBo bo, HttpServletResponse response) throws IOException {
        byte[] csv = reportService.export(bo);
        response.setContentType(CSV_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + CSV_FILE_NAME
            + "\"; filename*=UTF-8''" + CSV_FILE_NAME);
        response.getOutputStream().write(csv);
    }
}
