package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.service.reconciliation.ReconciliationPlatformEventProjection;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.bo.PaymentWebhookEventAdminBo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventAdminVo;
import com.gameluck.payment.domain.vo.PaymentWebhookEventDetailVo;
import com.gameluck.payment.service.settlement.SettlementSourceEvent;

public interface PaymentWebhookEventMapper extends BaseMapper<PaymentWebhookEvent> {

    @Select("<script>select w.id as webhook_event_id,w.provider_code,w.provider_event_id,w.event_type,w.status as webhook_status,w.provider_session_no,s.id as session_id,s.session_no,s.purchase_order_no as session_purchase_order_no,s.pay_currency_code as session_currency_code,s.pay_amount as session_amount,o.id as order_id,o.purchase_order_no as order_purchase_order_no,o.provider_code as order_provider_code,o.pay_currency_code as order_currency_code,o.pay_amount as order_amount,w.received_time from gl_payment_webhook_event w left join gl_payment_session s on s.tenant_id=w.tenant_id and s.provider_code=w.provider_code and s.provider_session_no=w.provider_session_no left join gl_purchase_order o on o.tenant_id=w.tenant_id and o.purchase_order_no=w.purchase_order_no where w.tenant_id=#{tenantId} and w.provider_code=#{providerCode} and w.status='PROCESSED' and w.received_time &gt;= #{periodStart} and w.received_time &lt; #{periodEnd}<if test='cursorReceivedTime != null'> and (w.received_time &gt; #{cursorReceivedTime} or (w.received_time=#{cursorReceivedTime} and w.id &gt; #{cursorId}))</if> order by w.received_time,w.id limit #{limit}</script>")
    List<SettlementSourceEvent> selectSettlementSourceEvents(@Param("tenantId") String tenantId,
        @Param("providerCode") String providerCode, @Param("periodStart") Instant periodStart,
        @Param("periodEnd") Instant periodEnd, @Param("cursorReceivedTime") Instant cursorReceivedTime,
        @Param("cursorId") Long cursorId, @Param("limit") int limit);

    @Select("<script>select w.id,w.provider_event_id,w.event_type,w.provider_session_no,w.purchase_order_no,"
        + "s.pay_currency_code as currency,s.pay_amount as amount,w.received_time as occurred_time,w.status,"
        + "s.id as payment_session_id,s.purchase_order_id,r.id as reversal_id,w.received_time from gl_payment_webhook_event w "
        + "left join gl_payment_session s on s.tenant_id=w.tenant_id and s.provider_code=w.provider_code and s.provider_session_no=w.provider_session_no "
        + "left join (select rr.id,rr.tenant_id,rr.purchase_order_no from (select r0.id,r0.tenant_id,r0.purchase_order_no,r0.create_time,"
        + "row_number() over(partition by r0.tenant_id,r0.purchase_order_no order by r0.create_time desc,r0.id desc) rn "
        + "from gl_purchase_reversal r0 where r0.tenant_id=#{tenantId}) rr "
        + "where rr.rn=1) r on r.tenant_id=w.tenant_id and r.purchase_order_no=w.purchase_order_no "
        + "where w.tenant_id = #{tenantId} "
        + "and w.provider_code = #{providerCode} and w.received_time &gt;= #{windowStart} "
        + "and w.received_time &lt; #{windowNext} "
        + "and not exists (select 1 from gl_payment_reconciliation_line rl where rl.tenant_id=#{tenantId} "
        + "and rl.batch_id=#{batchId} and rl.provider_record_id=w.provider_event_id and rl.status in ('VALID','MATCHED','ISSUE')) "
        + "<if test='cursorReceivedTime != null'> and (w.received_time &gt; #{cursorReceivedTime} "
        + "or (w.received_time = #{cursorReceivedTime} and w.id &gt; #{cursorId}))</if> "
        + "order by w.received_time,w.id limit #{limit}</script>")
    List<ReconciliationPlatformEventProjection> selectReconciliationStatementEvents(
        @Param("tenantId") String tenantId,
        @Param("batchId") Long batchId,
        @Param("providerCode") String providerCode,
        @Param("windowStart") Instant windowStart,
        @Param("windowNext") Instant windowNext,
        @Param("cursorReceivedTime") Instant cursorReceivedTime,
        @Param("cursorId") Long cursorId,
        @Param("limit") int limit);

    @Select("<script>select id,provider_code,provider_event_id,event_type,provider_session_no,session_no,purchase_order_no,received_time,status,failure_reason,processing_count,last_processing_time,create_time,update_time from gl_payment_webhook_event where tenant_id=#{tenantId}<if test='bo.providerEventId != null and bo.providerEventId != \"\"'> and provider_event_id=#{bo.providerEventId}</if><if test='bo.purchaseOrderNo != null and bo.purchaseOrderNo != \"\"'> and purchase_order_no=#{bo.purchaseOrderNo}</if><if test='bo.sessionNo != null and bo.sessionNo != \"\"'> and session_no=#{bo.sessionNo}</if><if test='bo.providerSessionNo != null and bo.providerSessionNo != \"\"'> and provider_session_no=#{bo.providerSessionNo}</if><if test='bo.eventType != null and bo.eventType != \"\"'> and event_type=#{bo.eventType}</if><if test='bo.status != null and bo.status != \"\"'> and status=#{bo.status}</if><if test='bo.providerCode != null and bo.providerCode != \"\"'> and provider_code=#{bo.providerCode}</if><if test='bo.beginTime != null'> and received_time &gt;=#{bo.beginTime}</if><if test='bo.endTime != null'> and received_time &lt;=#{bo.endTime}</if> order by received_time desc,id desc</script>")
    Page<PaymentWebhookEventAdminVo> selectAdminPage(Page<PaymentWebhookEventAdminVo> page,
        @Param("tenantId") String tenantId, @Param("bo") PaymentWebhookEventAdminBo bo);

    @Select("select id,provider_code,provider_event_id,event_type,provider_session_no,session_no,purchase_order_no,raw_body,signature_digest,received_time,status,failure_reason,processing_count,last_processing_time,create_time,update_time from gl_payment_webhook_event where tenant_id=#{tenantId} and id=#{id} limit 1")
    PaymentWebhookEventDetailVo selectAdminById(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("select * from gl_payment_webhook_event where tenant_id = #{tenantId} and provider_code = #{providerCode} and provider_session_no = #{providerSessionNo} order by received_time desc, id desc limit 1")
    PaymentWebhookEvent selectLatestByProviderSessionNo(@Param("tenantId") String tenantId,
                                                         @Param("providerCode") String providerCode,
                                                         @Param("providerSessionNo") String providerSessionNo);

    @Select("select * from gl_payment_webhook_event where tenant_id = #{tenantId} and provider_code = #{providerCode} and provider_event_id = #{providerEventId} limit 1")
    PaymentWebhookEvent selectByProviderEventId(@Param("tenantId") String tenantId,
                                                 @Param("providerCode") String providerCode,
                                                 @Param("providerEventId") String providerEventId);

    @Select("<script>select * from gl_payment_webhook_event where tenant_id=#{tenantId} and provider_code=#{providerCode} and provider_event_id in <foreach collection='providerEventIds' item='v' open='(' separator=',' close=')'>#{v}</foreach></script>")
    List<PaymentWebhookEvent> selectReconciliationByProviderEventIds(@Param("tenantId") String tenantId,
        @Param("providerCode") String providerCode, @Param("providerEventIds") List<String> providerEventIds);

    @Select("select * from gl_payment_webhook_event where tenant_id = #{tenantId} and provider_code = #{providerCode} and provider_event_id = #{providerEventId} limit 1 for update")
    PaymentWebhookEvent selectByProviderEventIdForUpdate(@Param("tenantId") String tenantId,
                                                          @Param("providerCode") String providerCode,
                                                          @Param("providerEventId") String providerEventId);

    @Select("select * from gl_payment_webhook_event where tenant_id = #{tenantId} and id = #{id} limit 1 for update")
    PaymentWebhookEvent selectByIdForUpdate(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Update("update gl_payment_webhook_event set status = #{newStatus}, failure_reason = null, processing_count = processing_count + 1, last_processing_time = #{now}, update_time = #{now} where tenant_id = #{tenantId} and id = #{id} and status = #{expectedStatus}")
    int completeProcessing(@Param("tenantId") String tenantId, @Param("id") Long id,
                           @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus,
                           @Param("now") Date now);

    @Update("update gl_payment_webhook_event set status = 'FAILED', failure_reason = #{failureReason}, processing_count = processing_count + 1, last_processing_time = #{now}, update_time = #{now} where tenant_id = #{tenantId} and id = #{id} and status in ('RECEIVED', 'FAILED')")
    int recordFailure(@Param("tenantId") String tenantId, @Param("id") Long id,
                      @Param("failureReason") String failureReason, @Param("now") Date now);
}
