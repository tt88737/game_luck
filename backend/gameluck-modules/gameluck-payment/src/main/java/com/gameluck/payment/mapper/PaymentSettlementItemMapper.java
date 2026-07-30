package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentSettlementItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface PaymentSettlementItemMapper {
    @Insert("insert into gl_payment_settlement_item (id,tenant_id,batch_id,webhook_event_id,provider_event_id,payment_session_id,session_no,provider_session_no,purchase_order_id,purchase_order_no,event_type,received_time,currency_code,source_amount,gross_payment,refund_amount,chargeback_amount,fee_amount,net_contribution,source_snapshot_json,create_time) values (#{entity.id},#{entity.tenantId},#{entity.batchId},#{entity.webhookEventId},#{entity.providerEventId},#{entity.paymentSessionId},#{entity.sessionNo},#{entity.providerSessionNo},#{entity.purchaseOrderId},#{entity.purchaseOrderNo},#{entity.eventType},#{entity.receivedTime},#{entity.currencyCode},#{entity.sourceAmount},#{entity.grossPayment},#{entity.refundAmount},#{entity.chargebackAmount},#{entity.feeAmount},#{entity.netContribution},#{entity.sourceSnapshotJson},COALESCE(#{entity.createTime},CURRENT_TIMESTAMP))")
    int insert(@Param("entity") PaymentSettlementItem entity);

    @Insert("<script>insert into gl_payment_settlement_item (id,tenant_id,batch_id,webhook_event_id,provider_event_id,payment_session_id,session_no,provider_session_no,purchase_order_id,purchase_order_no,event_type,received_time,currency_code,source_amount,gross_payment,refund_amount,chargeback_amount,fee_amount,net_contribution,source_snapshot_json,create_time) values <foreach collection='entities' item='e' separator=','>(#{e.id},#{e.tenantId},#{e.batchId},#{e.webhookEventId},#{e.providerEventId},#{e.paymentSessionId},#{e.sessionNo},#{e.providerSessionNo},#{e.purchaseOrderId},#{e.purchaseOrderNo},#{e.eventType},#{e.receivedTime},#{e.currencyCode},#{e.sourceAmount},#{e.grossPayment},#{e.refundAmount},#{e.chargebackAmount},#{e.feeAmount},#{e.netContribution},#{e.sourceSnapshotJson},COALESCE(#{e.createTime},CURRENT_TIMESTAMP))</foreach></script>")
    int insertBatch(@Param("entities") List<PaymentSettlementItem> entities);

    @Select("<script>select * from gl_payment_settlement_item where tenant_id=#{tenantId} and batch_id=#{batchId}<if test='eventType != null and eventType != \"\"'> and event_type=#{eventType}</if> order by received_time,id</script>")
    Page<PaymentSettlementItem> selectPageByBatch(Page<PaymentSettlementItem> page,
        @Param("tenantId") String tenantId, @Param("batchId") Long batchId,
        @Param("eventType") String eventType);
}
