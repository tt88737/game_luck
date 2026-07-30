package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.Date;

public interface PaymentSettlementBatchMapper {
    @Insert("insert into gl_payment_settlement_batch (id,tenant_id,settlement_no,provider_code,currency_code,period_start,period_end,status,payment_fee_rate,payment_fixed_fee,chargeback_fixed_fee,event_count,payment_count,refund_count,chargeback_count,gross_payment,refund_amount,chargeback_amount,total_fee,net_settlement,reconciliation_coverage_count,open_issue_count,evidence_snapshot_json,failure_reason,creator_id,creator_name,calculator_id,calculator_name,closer_id,closer_name,close_remark,calculated_time,closed_time,version,create_time,update_time) values (#{entity.id},#{entity.tenantId},#{entity.settlementNo},#{entity.providerCode},#{entity.currencyCode},#{entity.periodStart},#{entity.periodEnd},#{entity.status},#{entity.paymentFeeRate},#{entity.paymentFixedFee},#{entity.chargebackFixedFee},COALESCE(#{entity.eventCount},0),COALESCE(#{entity.paymentCount},0),COALESCE(#{entity.refundCount},0),COALESCE(#{entity.chargebackCount},0),COALESCE(#{entity.grossPayment},0),COALESCE(#{entity.refundAmount},0),COALESCE(#{entity.chargebackAmount},0),COALESCE(#{entity.totalFee},0),COALESCE(#{entity.netSettlement},0),COALESCE(#{entity.reconciliationCoverageCount},0),COALESCE(#{entity.openIssueCount},0),#{entity.evidenceSnapshotJson},#{entity.failureReason},#{entity.creatorId},#{entity.creatorName},#{entity.calculatorId},#{entity.calculatorName},#{entity.closerId},#{entity.closerName},#{entity.closeRemark},#{entity.calculatedTime},#{entity.closedTime},COALESCE(#{entity.version},0),COALESCE(#{entity.createTime},CURRENT_TIMESTAMP),#{entity.updateTime})")
    int insert(@Param("entity") PaymentSettlementBatch entity);

    @Select("select * from gl_payment_settlement_batch where tenant_id=#{tenantId} and id=#{id} limit 1")
    PaymentSettlementBatch selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("<script>select * from gl_payment_settlement_batch where tenant_id=#{tenantId}<if test='providerCode != null and providerCode != \"\"'> and provider_code=#{providerCode}</if><if test='currencyCode != null and currencyCode != \"\"'> and currency_code=#{currencyCode}</if><if test='status != null and status != \"\"'> and status=#{status}</if> order by create_time desc,id desc</script>")
    Page<PaymentSettlementBatch> selectPageByTenant(Page<PaymentSettlementBatch> page,
        @Param("tenantId") String tenantId, @Param("providerCode") String providerCode,
        @Param("currencyCode") String currencyCode, @Param("status") String status);

    @Select("<script>select count(*) from gl_payment_settlement_batch where tenant_id=#{tenantId} and provider_code=#{providerCode} and currency_code=#{currencyCode} and period_start &lt; #{periodEnd} and period_end &gt; #{periodStart} and status &lt;&gt; 'FAILED'<if test='excludedId != null'> and id &lt;&gt; #{excludedId}</if></script>")
    int countOverlapping(@Param("tenantId") String tenantId, @Param("providerCode") String providerCode,
        @Param("currencyCode") String currencyCode, @Param("periodStart") Date periodStart,
        @Param("periodEnd") Date periodEnd, @Param("excludedId") Long excludedId);

    @Update("update gl_payment_settlement_batch set status=#{next},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status=#{expected}")
    int transitionStatus(@Param("tenantId") String tenantId, @Param("id") Long id,
        @Param("expected") String expected, @Param("next") String next, @Param("now") Date now);

    @Update("update gl_payment_settlement_batch set status='CALCULATED',event_count=#{eventCount},payment_count=#{paymentCount},refund_count=#{refundCount},chargeback_count=#{chargebackCount},gross_payment=#{grossPayment},refund_amount=#{refundAmount},chargeback_amount=#{chargebackAmount},total_fee=#{totalFee},net_settlement=#{netSettlement},evidence_snapshot_json=#{evidenceJson},calculator_id=#{calculatorId},calculator_name=#{calculatorName},calculated_time=#{now},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='CALCULATING' and version=#{expectedVersion}")
    int completeCalculation(@Param("tenantId") String tenantId, @Param("id") Long id,
        @Param("expectedVersion") int expectedVersion, @Param("eventCount") int eventCount,
        @Param("paymentCount") int paymentCount, @Param("refundCount") int refundCount,
        @Param("chargebackCount") int chargebackCount, @Param("grossPayment") BigDecimal grossPayment,
        @Param("refundAmount") BigDecimal refundAmount, @Param("chargebackAmount") BigDecimal chargebackAmount,
        @Param("totalFee") BigDecimal totalFee, @Param("netSettlement") BigDecimal netSettlement,
        @Param("evidenceJson") String evidenceJson, @Param("calculatorId") Long calculatorId,
        @Param("calculatorName") String calculatorName, @Param("now") Date now);

    @Update("update gl_payment_settlement_batch set status='FAILED',failure_reason=#{reason},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='CALCULATING'")
    int markFailed(@Param("tenantId") String tenantId, @Param("id") Long id,
        @Param("reason") String reason, @Param("now") Date now);

    @Update("update gl_payment_settlement_batch set status='CLOSED',reconciliation_coverage_count=#{coverageCount},open_issue_count=#{openIssueCount},evidence_snapshot_json=#{evidenceJson},closer_id=#{operatorId},closer_name=#{operatorName},close_remark=#{remark},closed_time=#{now},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='CALCULATED' and version=#{expectedVersion}")
    int closeCalculated(@Param("tenantId") String tenantId, @Param("id") Long id,
        @Param("expectedVersion") int expectedVersion, @Param("coverageCount") int coverageCount,
        @Param("openIssueCount") int openIssueCount, @Param("evidenceJson") String evidenceJson,
        @Param("operatorId") Long operatorId, @Param("operatorName") String operatorName,
        @Param("remark") String remark, @Param("now") Date now);
}
