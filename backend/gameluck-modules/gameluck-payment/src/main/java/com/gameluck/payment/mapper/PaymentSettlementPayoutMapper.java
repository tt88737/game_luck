package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentSettlementPayout;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

public interface PaymentSettlementPayoutMapper {
    @Insert("insert into gl_payment_settlement_payout (id,tenant_id,payout_no,settlement_batch_id,settlement_no,provider_code,currency_code,payout_amount,settlement_evidence_json,payout_purpose,payee_reference,status,maker_id,maker_name,submitter_id,submitter_name,reviewer_id,reviewer_name,decision_reason,version,submitted_time,reviewed_time,create_time,update_time) values (#{entity.id},#{entity.tenantId},#{entity.payoutNo},#{entity.settlementBatchId},#{entity.settlementNo},#{entity.providerCode},#{entity.currencyCode},#{entity.payoutAmount},#{entity.settlementEvidenceJson},#{entity.payoutPurpose},#{entity.payeeReference},#{entity.status},#{entity.makerId},#{entity.makerName},#{entity.submitterId},#{entity.submitterName},#{entity.reviewerId},#{entity.reviewerName},#{entity.decisionReason},COALESCE(#{entity.version},0),#{entity.submittedTime},#{entity.reviewedTime},COALESCE(#{entity.createTime},CURRENT_TIMESTAMP),#{entity.updateTime})")
    int insert(@Param("entity") PaymentSettlementPayout entity);

    @Select("select * from gl_payment_settlement_payout where tenant_id=#{tenantId} and id=#{id} limit 1")
    PaymentSettlementPayout selectByTenantAndId(@Param("tenantId") String tenantId,
        @Param("id") Long id);

    @Select("select * from gl_payment_settlement_payout where tenant_id=#{tenantId} and settlement_batch_id=#{batchId} limit 1")
    PaymentSettlementPayout selectByTenantAndBatchId(@Param("tenantId") String tenantId,
        @Param("batchId") Long batchId);

    @Select("<script>select * from gl_payment_settlement_payout where tenant_id=#{tenantId}"
        + "<if test='payoutNo != null and payoutNo != \"\"'> and payout_no=#{payoutNo}</if>"
        + "<if test='settlementNo != null and settlementNo != \"\"'> and settlement_no=#{settlementNo}</if>"
        + "<if test='status != null and status != \"\"'> and status=#{status}</if>"
        + "<if test='providerCode != null and providerCode != \"\"'> and provider_code=#{providerCode}</if>"
        + "<if test='currencyCode != null and currencyCode != \"\"'> and currency_code=#{currencyCode}</if>"
        + "<if test='start != null'> and create_time &gt;= #{start}</if>"
        + "<if test='end != null'> and create_time &lt; #{end}</if>"
        + " order by create_time desc,id desc</script>")
    Page<PaymentSettlementPayout> selectPageByTenant(Page<?> page,
        @Param("tenantId") String tenantId, @Param("payoutNo") String payoutNo,
        @Param("settlementNo") String settlementNo, @Param("status") String status,
        @Param("providerCode") String providerCode, @Param("currencyCode") String currencyCode,
        @Param("start") Date start, @Param("end") Date end);

    @Update("update gl_payment_settlement_payout set payout_purpose=#{purpose},payee_reference=#{payeeReference},status='DRAFT',decision_reason=null,version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and version=#{version} and status in ('DRAFT','REJECTED')")
    int editDraftOrRejected(@Param("tenantId") String tenantId, @Param("id") Long id,
        @Param("version") int version, @Param("purpose") String purpose,
        @Param("payeeReference") String payeeReference, @Param("now") Date now);

    @Update("update gl_payment_settlement_payout set status=#{next},submitter_id=CASE WHEN #{next}='PENDING_APPROVAL' THEN #{operatorId} ELSE submitter_id END,submitter_name=CASE WHEN #{next}='PENDING_APPROVAL' THEN #{operatorName} ELSE submitter_name END,submitted_time=CASE WHEN #{next}='PENDING_APPROVAL' THEN #{now} ELSE submitted_time END,reviewer_id=CASE WHEN #{next} IN ('APPROVED','REJECTED') THEN #{operatorId} ELSE reviewer_id END,reviewer_name=CASE WHEN #{next} IN ('APPROVED','REJECTED') THEN #{operatorName} ELSE reviewer_name END,decision_reason=CASE WHEN #{next} IN ('APPROVED','REJECTED','CANCELLED') THEN #{reason} ELSE decision_reason END,reviewed_time=CASE WHEN #{next} IN ('APPROVED','REJECTED') THEN #{now} ELSE reviewed_time END,version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and version=#{version} and status=#{expected}")
    int transition(@Param("tenantId") String tenantId, @Param("id") Long id,
        @Param("version") int version, @Param("expected") String expected,
        @Param("next") String next, @Param("operatorId") Long operatorId,
        @Param("operatorName") String operatorName, @Param("reason") String reason,
        @Param("now") Date now);
}
