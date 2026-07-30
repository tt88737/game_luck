package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import com.gameluck.payment.domain.bo.PaymentReconciliationIssueBo;

public interface PaymentReconciliationIssueMapper {
    @Select("<script>select status,provider_currency_code,platform_currency_code from gl_payment_reconciliation_issue where tenant_id=#{tenantId} and batch_id in <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<PaymentReconciliationIssue> selectByReconciliationBatches(@Param("tenantId") String tenantId,
        @Param("batchIds") List<Long> batchIds);

    @Insert("insert into gl_payment_reconciliation_issue (id,tenant_id,batch_id,line_id,issue_type,status,payment_session_id,session_no,purchase_order_id,purchase_order_no,webhook_event_id,reversal_id,provider_event_type,platform_event_type,provider_currency_code,platform_currency_code,provider_amount,platform_amount,provider_status,platform_status,diagnostic_snapshot_json,resolution_type,resolution_remark,resolved_by,resolved_time,version,create_time,update_time) values (#{entity.id},#{entity.tenantId},#{entity.batchId},#{entity.lineId},#{entity.issueType},#{entity.status},#{entity.paymentSessionId},#{entity.sessionNo},#{entity.purchaseOrderId},#{entity.purchaseOrderNo},#{entity.webhookEventId},#{entity.reversalId},#{entity.providerEventType},#{entity.platformEventType},#{entity.providerCurrencyCode},#{entity.platformCurrencyCode},#{entity.providerAmount},#{entity.platformAmount},#{entity.providerStatus},#{entity.platformStatus},#{entity.diagnosticSnapshotJson},#{entity.resolutionType},#{entity.resolutionRemark},#{entity.resolvedBy},#{entity.resolvedTime},COALESCE(#{entity.version},0),COALESCE(#{entity.createTime},CURRENT_TIMESTAMP),#{entity.updateTime})")
    int insert(@Param("entity") PaymentReconciliationIssue entity);

    @Insert("<script>insert into gl_payment_reconciliation_issue (id,tenant_id,batch_id,line_id,issue_type,status,payment_session_id,session_no,purchase_order_id,purchase_order_no,webhook_event_id,reversal_id,provider_event_type,platform_event_type,provider_currency_code,platform_currency_code,provider_amount,platform_amount,provider_status,platform_status,diagnostic_snapshot_json,resolution_type,resolution_remark,resolved_by,resolved_time,version,create_time,update_time) values <foreach collection='entities' item='e' separator=','>(#{e.id},#{e.tenantId},#{e.batchId},#{e.lineId},#{e.issueType},#{e.status},#{e.paymentSessionId},#{e.sessionNo},#{e.purchaseOrderId},#{e.purchaseOrderNo},#{e.webhookEventId},#{e.reversalId},#{e.providerEventType},#{e.platformEventType},#{e.providerCurrencyCode},#{e.platformCurrencyCode},#{e.providerAmount},#{e.platformAmount},#{e.providerStatus},#{e.platformStatus},#{e.diagnosticSnapshotJson},#{e.resolutionType},#{e.resolutionRemark},#{e.resolvedBy},#{e.resolvedTime},COALESCE(#{e.version},0),COALESCE(#{e.createTime},CURRENT_TIMESTAMP),#{e.updateTime})</foreach></script>")
    int insertBatch(@Param("entities") List<PaymentReconciliationIssue> entities);

    @Select("<script>select i.* from gl_payment_reconciliation_issue i left join gl_payment_reconciliation_line l on l.tenant_id=i.tenant_id and l.id=i.line_id where i.tenant_id = #{tenantId} and i.batch_id = #{batchId}<if test='bo.issueType != null and bo.issueType != \"\"'> and i.issue_type=#{bo.issueType}</if><if test='bo.status != null and bo.status != \"\"'> and i.status=#{bo.status}</if><if test='bo.purchaseOrderNo != null and bo.purchaseOrderNo != \"\"'> and i.purchase_order_no=#{bo.purchaseOrderNo}</if><if test='bo.sessionNo != null and bo.sessionNo != \"\"'> and i.session_no=#{bo.sessionNo}</if><if test='bo.providerRecordId != null and bo.providerRecordId != \"\"'> and l.provider_record_id=#{bo.providerRecordId}</if> order by i.create_time desc,i.id desc</script>")
    Page<PaymentReconciliationIssue> selectPageByBatchFiltered(Page<PaymentReconciliationIssue> page,
                                                        @Param("tenantId") String tenantId,
                                                        @Param("batchId") Long batchId,
                                                        @Param("bo") PaymentReconciliationIssueBo bo);

    @Select("select * from gl_payment_reconciliation_issue where tenant_id = #{tenantId} and batch_id = #{batchId} order by create_time desc,id desc")
    Page<PaymentReconciliationIssue> selectPageByBatch(Page<PaymentReconciliationIssue> page,
                                                        @Param("tenantId") String tenantId,
                                                        @Param("batchId") Long batchId);

    @Select("select * from gl_payment_reconciliation_issue where tenant_id = #{tenantId} and id = #{id} limit 1")
    PaymentReconciliationIssue selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Update("update gl_payment_reconciliation_issue set status = #{nextStatus}, resolution_type = #{resolutionType}, resolution_remark = #{remark}, resolved_by = #{operatorId}, resolved_time = #{resolvedTime}, version = version + 1, update_time = #{resolvedTime} where tenant_id = #{tenantId} and id = #{id} and status = 'OPEN' and version = #{expectedVersion}")
    int resolveOpenIssue(@Param("tenantId") String tenantId, @Param("id") Long id,
                         @Param("expectedVersion") Integer expectedVersion, @Param("nextStatus") String nextStatus,
                         @Param("resolutionType") String resolutionType, @Param("remark") String remark,
                         @Param("operatorId") Long operatorId, @Param("resolvedTime") Date resolvedTime);
}
