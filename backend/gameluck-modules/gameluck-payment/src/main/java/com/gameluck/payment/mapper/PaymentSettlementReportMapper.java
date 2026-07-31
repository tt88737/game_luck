package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportCurrencyTotalVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface PaymentSettlementReportMapper {

    String FILTER = " from gl_payment_settlement_batch where tenant_id=#{tenantId} and status='CLOSED'"
        + " and period_start &gt;= #{periodStart} and period_start &lt; #{periodEndExclusive}"
        + "<if test='providerCode != null'> and provider_code=#{providerCode}</if>"
        + "<if test='currencyCode != null'> and currency_code=#{currencyCode}</if>";

    String GROUP_COLUMNS = "DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')),provider_code,currency_code";

    @Select("<script>select DATE_FORMAT(DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')),'%Y-%m-%d') settlement_date,"
        + "provider_code,currency_code,COUNT(*) batch_count,SUM(event_count) event_count,"
        + "SUM(payment_count) payment_count,SUM(refund_count) refund_count,SUM(chargeback_count) chargeback_count,"
        + "CAST(SUM(gross_payment) AS CHAR) gross_payment,CAST(SUM(refund_amount) AS CHAR) refund_amount,"
        + "CAST(SUM(chargeback_amount) AS CHAR) chargeback_amount,CAST(SUM(total_fee) AS CHAR) total_fee,"
        + "CAST(SUM(net_settlement) AS CHAR) net_settlement,(SUM(net_settlement)&lt;0) negative_net,"
        + "DATE_FORMAT(MIN(period_start),'%Y-%m-%dT%H:%i:%sZ') earliest_period_start,"
        + "DATE_FORMAT(MAX(period_end),'%Y-%m-%dT%H:%i:%sZ') latest_period_end,"
        + "DATE_FORMAT(MAX(closed_time),'%Y-%m-%dT%H:%i:%sZ') latest_close_time"
        + FILTER + " group by " + GROUP_COLUMNS
        + " order by DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')) desc,provider_code,currency_code</script>")
    Page<PaymentSettlementReportRowVo> selectGroupedRows(Page<PaymentSettlementReportRowVo> page,
        @Param("tenantId") String tenantId, @Param("periodStart") Date periodStart,
        @Param("periodEndExclusive") Date periodEndExclusive, @Param("providerCode") String providerCode,
        @Param("currencyCode") String currencyCode);

    @Select("<script>select count(*) from (select 1" + FILTER + " group by " + GROUP_COLUMNS
        + ") settlement_report_groups</script>")
    long countGroupedRows(@Param("tenantId") String tenantId, @Param("periodStart") Date periodStart,
        @Param("periodEndExclusive") Date periodEndExclusive, @Param("providerCode") String providerCode,
        @Param("currencyCode") String currencyCode);

    @Select("<script>select DATE_FORMAT(DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')),'%Y-%m-%d') settlement_date,"
        + "provider_code,currency_code,COUNT(*) batch_count,SUM(event_count) event_count,"
        + "SUM(payment_count) payment_count,SUM(refund_count) refund_count,SUM(chargeback_count) chargeback_count,"
        + "CAST(SUM(gross_payment) AS CHAR) gross_payment,CAST(SUM(refund_amount) AS CHAR) refund_amount,"
        + "CAST(SUM(chargeback_amount) AS CHAR) chargeback_amount,CAST(SUM(total_fee) AS CHAR) total_fee,"
        + "CAST(SUM(net_settlement) AS CHAR) net_settlement,(SUM(net_settlement)&lt;0) negative_net,"
        + "DATE_FORMAT(MIN(period_start),'%Y-%m-%dT%H:%i:%sZ') earliest_period_start,"
        + "DATE_FORMAT(MAX(period_end),'%Y-%m-%dT%H:%i:%sZ') latest_period_end,"
        + "DATE_FORMAT(MAX(closed_time),'%Y-%m-%dT%H:%i:%sZ') latest_close_time"
        + FILTER + " group by " + GROUP_COLUMNS
        + " order by DATE(CONVERT_TZ(period_start,@@session.time_zone,'+00:00')) desc,provider_code,currency_code</script>")
    List<PaymentSettlementReportRowVo> selectExportRows(@Param("tenantId") String tenantId,
        @Param("periodStart") Date periodStart, @Param("periodEndExclusive") Date periodEndExclusive,
        @Param("providerCode") String providerCode, @Param("currencyCode") String currencyCode);

    @Select("<script>select currency_code,COUNT(*) batch_count,SUM(event_count) event_count,"
        + "SUM(payment_count) payment_count,SUM(refund_count) refund_count,SUM(chargeback_count) chargeback_count,"
        + "CAST(SUM(gross_payment) AS CHAR) gross_payment,CAST(SUM(refund_amount) AS CHAR) refund_amount,"
        + "CAST(SUM(chargeback_amount) AS CHAR) chargeback_amount,CAST(SUM(total_fee) AS CHAR) total_fee,"
        + "CAST(SUM(net_settlement) AS CHAR) net_settlement" + FILTER
        + " group by currency_code order by currency_code</script>")
    List<PaymentSettlementReportCurrencyTotalVo> selectCurrencyTotals(@Param("tenantId") String tenantId,
        @Param("periodStart") Date periodStart, @Param("periodEndExclusive") Date periodEndExclusive,
        @Param("providerCode") String providerCode, @Param("currencyCode") String currencyCode);

    @Select("<script>select cast(id as char) id,settlement_no,provider_code,currency_code,period_start,period_end,status,"
        + "cast(payment_fee_rate as char) payment_fee_rate,cast(payment_fixed_fee as char) payment_fixed_fee,"
        + "cast(chargeback_fixed_fee as char) chargeback_fixed_fee,event_count,payment_count,refund_count,chargeback_count,"
        + "cast(gross_payment as char) gross_payment,cast(refund_amount as char) refund_amount,"
        + "cast(chargeback_amount as char) chargeback_amount,cast(total_fee as char) total_fee,"
        + "cast(net_settlement as char) net_settlement,reconciliation_coverage_count,open_issue_count,failure_reason,"
        + "cast(creator_id as char) creator_id,creator_name,cast(calculator_id as char) calculator_id,calculator_name,"
        + "cast(closer_id as char) closer_id,closer_name,close_remark,calculated_time,closed_time,version,create_time,update_time"
        + " from gl_payment_settlement_batch where tenant_id=#{tenantId} and status='CLOSED'"
        + " and period_start &gt;= #{periodStart} and period_start &lt; #{periodEndExclusive}"
        + " and provider_code=#{providerCode} and currency_code=#{currencyCode} order by period_start,id</script>")
    List<PaymentSettlementBatchVo> selectGroupBatches(@Param("tenantId") String tenantId,
        @Param("periodStart") Date periodStart, @Param("periodEndExclusive") Date periodEndExclusive,
        @Param("providerCode") String providerCode, @Param("currencyCode") String currencyCode);
}
