package com.gameluck.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentReconciliationBatchBo;
import com.gameluck.payment.domain.bo.PaymentReconciliationIssueBo;
import com.gameluck.payment.domain.bo.PaymentReconciliationResolutionBo;
import com.gameluck.payment.domain.vo.*;
import com.gameluck.payment.service.IPaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment/reconciliation")
public class PaymentReconciliationController {
    private final IPaymentReconciliationService reconciliationService;

    @SaCheckPermission("payment:reconciliation:list")
    @GetMapping("/list")
    public TableDataInfo<PaymentReconciliationBatchVo> list(PaymentReconciliationBatchBo bo, PageQuery pageQuery) {
        return reconciliationService.queryPage(bo, pageQuery);
    }

    @SaCheckPermission("payment:reconciliation:upload")
    @Log(title = "Payment reconciliation upload", businessType = BusinessType.IMPORT,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public R<PaymentReconciliationBatchDetailVo> upload(@RequestParam String providerCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statementDate,
            @RequestPart("file") MultipartFile file) throws IOException {
        return R.ok(reconciliationService.upload(providerCode, statementDate, file.getOriginalFilename(),
            file.getSize(), file.getInputStream()));
    }

    @SaCheckPermission("payment:reconciliation:query")
    @GetMapping("/{batchId}")
    public R<PaymentReconciliationBatchDetailVo> detail(@PathVariable Long batchId) {
        return R.ok(reconciliationService.queryDetail(batchId));
    }

    @SaCheckPermission("payment:reconciliation:query")
    @GetMapping("/{batchId}/lines")
    public TableDataInfo<PaymentReconciliationLineVo> lines(@PathVariable Long batchId,
            @RequestParam(required = false) String lineStatus, PageQuery pageQuery) {
        return reconciliationService.queryLines(batchId, lineStatus, pageQuery);
    }

    @SaCheckPermission("payment:reconciliation:query")
    @GetMapping("/{batchId}/issues")
    public TableDataInfo<PaymentReconciliationIssueVo> issues(@PathVariable Long batchId,
            PaymentReconciliationIssueBo bo, PageQuery pageQuery) {
        return reconciliationService.queryIssues(batchId, bo, pageQuery);
    }

    @SaCheckPermission("payment:reconciliation:execute")
    @Log(title = "Payment reconciliation execute", businessType = BusinessType.UPDATE)
    @PostMapping("/{batchId}/execute")
    public R<PaymentReconciliationBatchDetailVo> execute(@PathVariable Long batchId) {
        return R.ok(reconciliationService.execute(batchId));
    }

    @SaCheckPermission("payment:reconciliation:query")
    @GetMapping("/issues/{issueId}")
    public R<PaymentReconciliationIssueDetailVo> issueDetail(@PathVariable Long issueId) {
        return R.ok(reconciliationService.queryIssueDetail(issueId));
    }

    @SaCheckPermission("payment:reconciliation:resolve")
    @Log(title = "Payment reconciliation resolve", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/issues/{issueId}/resolve")
    public R<PaymentReconciliationIssueDetailVo> resolve(@PathVariable Long issueId,
            @Validated @RequestBody PaymentReconciliationResolutionBo bo) {
        return R.ok(reconciliationService.resolve(issueId, bo));
    }

    @SaCheckPermission("payment:reconciliation:resolve")
    @Log(title = "Payment reconciliation ignore", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/issues/{issueId}/ignore")
    public R<PaymentReconciliationIssueDetailVo> ignore(@PathVariable Long issueId,
            @Validated @RequestBody PaymentReconciliationResolutionBo bo) {
        return R.ok(reconciliationService.ignore(issueId, bo));
    }
}
