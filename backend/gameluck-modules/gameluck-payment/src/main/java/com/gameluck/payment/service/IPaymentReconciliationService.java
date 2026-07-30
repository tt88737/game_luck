package com.gameluck.payment.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.payment.domain.bo.PaymentReconciliationBatchBo;
import com.gameluck.payment.domain.bo.PaymentReconciliationIssueBo;
import com.gameluck.payment.domain.bo.PaymentReconciliationResolutionBo;
import com.gameluck.payment.domain.vo.PaymentReconciliationBatchDetailVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationBatchVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationLineVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationIssueVo;
import com.gameluck.payment.domain.vo.PaymentReconciliationIssueDetailVo;
import java.io.InputStream;
import java.time.LocalDate;

public interface IPaymentReconciliationService {
    PaymentReconciliationBatchDetailVo upload(String providerCode, LocalDate statementDate, String originalFileName,
                                               long size, InputStream input);
    TableDataInfo<PaymentReconciliationBatchVo> queryPage(PaymentReconciliationBatchBo bo, PageQuery pageQuery);
    PaymentReconciliationBatchDetailVo queryDetail(Long batchId);
    TableDataInfo<PaymentReconciliationLineVo> queryLines(Long batchId, String lineStatus, PageQuery pageQuery);
    PaymentReconciliationBatchDetailVo execute(Long batchId);
    TableDataInfo<PaymentReconciliationIssueVo> queryIssues(Long batchId, PaymentReconciliationIssueBo bo, PageQuery pageQuery);
    PaymentReconciliationIssueDetailVo queryIssueDetail(Long issueId);
    PaymentReconciliationIssueDetailVo resolve(Long issueId, PaymentReconciliationResolutionBo bo);
    PaymentReconciliationIssueDetailVo ignore(Long issueId, PaymentReconciliationResolutionBo bo);
}
