package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.vo.PaymentSettlementReportCurrencyTotalVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface PaymentSettlementReportMapper {
    String FILTER = " tenant_id=#{tenantId} and status='CLOSED' and period_start&gt;=#{periodStart}"
        + " and period_start&lt;#{periodEndExclusive}"
        + "<if test='providerCode != null'> and provider_code=#{providerCode}</if>"
        + "<if test='currencyCode != null'> and currency_code=#{currencyCode}</if>";

    @Select("<script>select DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')) report_date,"
        + "provider_code,currency_code,count(*) batch_count,coalesce(sum(event_count),0) event_count,"
        + "coalesce(sum(payment_count),0) payment_event_count,coalesce(sum(refund_count),0) refund_event_count,"
        + "coalesce(sum(chargeback_count),0) chargeback_event_count,coalesce(sum(gross_payment),0) gross_payment,"
        + "coalesce(sum(refund_amount),0) refund_amount,coalesce(sum(chargeback_amount),0) chargeback_amount,"
        + "coalesce(sum(total_fee),0) total_fee,coalesce(sum(net_settlement),0) net_settlement,"
        + "case when coalesce(sum(net_settlement),0)&lt;0 then true else false end negative_net,"
        + "min(period_start) earliest_period_start,max(period_end) latest_period_end,max(closed_time) latest_close_time"
        + " from gl_payment_settlement_batch where" + FILTER
        + " group by DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')),provider_code,currency_code"
        + " order by report_date desc,provider_code asc,currency_code asc</script>")
    Page<PaymentSettlementReportRowVo> selectGroupedRows(Page<PaymentSettlementReportRowVo> page,
        @Param("tenantId") String tenantId, @Param("periodStart") Date periodStart,
        @Param("periodEndExclusive") Date periodEndExclusive, @Param("providerCode") String providerCode,
        @Param("currencyCode") String currencyCode);

    @Select("<script>select currency_code,count(*) batch_count,coalesce(sum(event_count),0) event_count,"
        + "coalesce(sum(payment_count),0) payment_event_count,coalesce(sum(refund_count),0) refund_event_count,"
        + "coalesce(sum(chargeback_count),0) chargeback_event_count,coalesce(sum(gross_payment),0) gross_payment,"
        + "coalesce(sum(refund_amount),0) refund_amount,coalesce(sum(chargeback_amount),0) chargeback_amount,"
        + "coalesce(sum(total_fee),0) total_fee,coalesce(sum(net_settlement),0) net_settlement"
        + " from gl_payment_settlement_batch where" + FILTER
        + " group by currency_code order by currency_code asc</script>")
    List<PaymentSettlementReportCurrencyTotalVo> selectCurrencyTotals(@Param("tenantId") String tenantId,
        @Param("periodStart") Date periodStart, @Param("periodEndExclusive") Date periodEndExclusive,
        @Param("providerCode") String providerCode, @Param("currencyCode") String currencyCode);

    @Select("select * from gl_payment_settlement_batch where tenant_id=#{tenantId} and status='CLOSED'"
        + " and DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00'))=#{reportDate}"
        + " and provider_code=#{providerCode} and currency_code=#{currencyCode} order by period_start asc,id asc")
    List<PaymentSettlementBatch> selectBatchesByGroup(@Param("tenantId") String tenantId,
        @Param("reportDate") LocalDate reportDate, @Param("providerCode") String providerCode,
        @Param("currencyCode") String currencyCode);
}
