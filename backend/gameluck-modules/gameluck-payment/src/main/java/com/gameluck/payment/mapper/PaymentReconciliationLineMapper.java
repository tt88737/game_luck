package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentReconciliationLine;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface PaymentReconciliationLineMapper {
    @Insert("insert into gl_payment_reconciliation_line (id,tenant_id,batch_id,source_row_number,provider_record_id,event_type,provider_session_no,purchase_order_no,currency_code,amount,occurred_time,status,parse_error,raw_fields_json,create_time) values (#{entity.id},#{entity.tenantId},#{entity.batchId},#{entity.sourceRowNumber},#{entity.providerRecordId},#{entity.eventType},#{entity.providerSessionNo},#{entity.purchaseOrderNo},#{entity.currencyCode},#{entity.amount},#{entity.occurredTime},#{entity.status},#{entity.parseError},#{entity.rawFieldsJson},COALESCE(#{entity.createTime},CURRENT_TIMESTAMP))")
    int insert(@Param("entity") PaymentReconciliationLine entity);

    @Insert("<script>insert into gl_payment_reconciliation_line (id,tenant_id,batch_id,source_row_number,provider_record_id,event_type,provider_session_no,purchase_order_no,currency_code,amount,occurred_time,status,parse_error,raw_fields_json,create_time) values " +
        "<foreach collection='entities' item='e' separator=','>(#{e.id},#{e.tenantId},#{e.batchId},#{e.sourceRowNumber},#{e.providerRecordId},#{e.eventType},#{e.providerSessionNo},#{e.purchaseOrderNo},#{e.currencyCode},#{e.amount},#{e.occurredTime},#{e.status},#{e.parseError},#{e.rawFieldsJson},#{e.createTime})</foreach></script>")
    int insertBatch(@Param("entities") List<PaymentReconciliationLine> entities);

    @Select("<script>select * from gl_payment_reconciliation_line where tenant_id = #{tenantId} and batch_id = #{batchId}" +
        "<if test='lineStatus != null and lineStatus != \"\"'> and status=#{lineStatus}</if> order by source_row_number,id</script>")
    Page<PaymentReconciliationLine> selectPageByBatch(Page<PaymentReconciliationLine> page,
                                                       @Param("tenantId") String tenantId,
                                                       @Param("batchId") Long batchId,
                                                       @Param("lineStatus") String lineStatus);

    @Select("select * from gl_payment_reconciliation_line where tenant_id=#{tenantId} and id=#{id}")
    PaymentReconciliationLine selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("select * from gl_payment_reconciliation_line where tenant_id=#{tenantId} and batch_id=#{batchId} and status='VALID' and id > #{cursorId} order by id limit #{limit}")
    List<PaymentReconciliationLine> selectValidChunk(@Param("tenantId") String tenantId,
        @Param("batchId") Long batchId, @Param("cursorId") Long cursorId, @Param("limit") int limit);

    @Select("<script>select distinct l.provider_record_id from gl_payment_reconciliation_line l " +
        "join gl_payment_reconciliation_batch b on b.tenant_id=l.tenant_id and b.id=l.batch_id " +
        "where l.tenant_id=#{tenantId} and l.batch_id&lt;&gt;#{batchId} and b.status='COMPLETED' " +
        "and l.provider_record_id in <foreach collection='providerRecordIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<String> selectProviderIdsFromOtherCompletedBatches(@Param("tenantId") String tenantId,
        @Param("batchId") Long batchId, @Param("providerRecordIds") List<String> providerRecordIds);

    @Update("update gl_payment_reconciliation_line set status=#{conclusion} where tenant_id=#{tenantId} and batch_id=#{batchId} and id=#{id} and status='VALID'")
    int concludeValidLine(@Param("tenantId") String tenantId, @Param("batchId") Long batchId,
                          @Param("id") Long id, @Param("conclusion") String conclusion);

    @Update("<script>update gl_payment_reconciliation_line set status=#{conclusion} where tenant_id=#{tenantId} and batch_id=#{batchId} and status='VALID' and id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int concludeValidLines(@Param("tenantId") String tenantId, @Param("batchId") Long batchId,
        @Param("ids") List<Long> ids, @Param("conclusion") String conclusion);
}
