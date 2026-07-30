package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.time.LocalDate;
import java.util.List;
import com.gameluck.payment.domain.bo.PaymentReconciliationBatchBo;

public interface PaymentReconciliationBatchMapper {
    @Select("select id,statement_date from gl_payment_reconciliation_batch where tenant_id=#{tenantId} and provider_code=#{providerCode} and status='COMPLETED' and statement_date between #{firstDate} and #{lastDate} order by statement_date,id")
    List<PaymentReconciliationBatch> selectCompletedForSettlement(@Param("tenantId") String tenantId,
        @Param("providerCode") String providerCode, @Param("firstDate") LocalDate firstDate,
        @Param("lastDate") LocalDate lastDate);

    @Insert("insert into gl_payment_reconciliation_batch (id,tenant_id,provider_code,statement_date,original_file_name,file_digest,total_count,valid_count,invalid_count,matched_count,discrepancy_count,status,failure_reason,creator_id,creator_name,version,create_time,update_time) values (#{entity.id},#{entity.tenantId},#{entity.providerCode},#{entity.statementDate},#{entity.originalFileName},#{entity.fileDigest},COALESCE(#{entity.totalCount},0),COALESCE(#{entity.validCount},0),COALESCE(#{entity.invalidCount},0),COALESCE(#{entity.matchedCount},0),COALESCE(#{entity.discrepancyCount},0),#{entity.status},#{entity.failureReason},#{entity.creatorId},#{entity.creatorName},COALESCE(#{entity.version},0),COALESCE(#{entity.createTime},CURRENT_TIMESTAMP),#{entity.updateTime})")
    int insert(@Param("entity") PaymentReconciliationBatch entity);

    @Select("<script>select * from gl_payment_reconciliation_batch where tenant_id = #{tenantId}" +
        "<if test='bo.providerCode != null and bo.providerCode != \"\"'> and provider_code=#{bo.providerCode}</if>" +
        "<if test='bo.statementDate != null'> and statement_date=#{bo.statementDate}</if>" +
        "<if test='bo.status != null and bo.status != \"\"'> and status=#{bo.status}</if>" +
        "<if test='bo.originalFileName != null and bo.originalFileName != \"\"'> and original_file_name like concat('%',#{bo.originalFileName},'%')</if>" +
        " order by create_time desc,id desc</script>")
    Page<PaymentReconciliationBatch> selectPageByTenant(Page<PaymentReconciliationBatch> page,
                                                        @Param("tenantId") String tenantId,
                                                        @Param("bo") PaymentReconciliationBatchBo bo);

    @Select("select * from gl_payment_reconciliation_batch where tenant_id = #{tenantId} and id = #{id} limit 1")
    PaymentReconciliationBatch selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("select * from gl_payment_reconciliation_batch where tenant_id = #{tenantId} and provider_code = #{providerCode} and file_digest = #{fileDigest} limit 1")
    PaymentReconciliationBatch selectByDigest(@Param("tenantId") String tenantId,
                                               @Param("providerCode") String providerCode,
                                               @Param("fileDigest") String fileDigest);

    @Update("update gl_payment_reconciliation_batch set status = #{nextStatus}, version = version + 1, update_time = #{now} where tenant_id = #{tenantId} and id = #{id} and status = #{expectedStatus}")
    int transitionStatus(@Param("tenantId") String tenantId, @Param("id") Long id,
                         @Param("expectedStatus") String expectedStatus, @Param("nextStatus") String nextStatus,
                         @Param("now") Date now);

    @Update("update gl_payment_reconciliation_batch set total_count=#{total},valid_count=#{valid},invalid_count=#{invalid},status='VALIDATED',version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='UPLOADED'")
    int finalizeValidation(@Param("tenantId") String tenantId, @Param("id") Long id,
                           @Param("total") int total, @Param("valid") int valid,
                           @Param("invalid") int invalid, @Param("now") Date now);

    @Update("update gl_payment_reconciliation_batch set status='FAILED',failure_reason=#{reason},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='UPLOADED'")
    int markFailed(@Param("tenantId") String tenantId, @Param("id") Long id,
                   @Param("reason") String reason, @Param("now") Date now);

    @Update("update gl_payment_reconciliation_batch set status='RECONCILING',failure_reason=null,version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='VALIDATED' and invalid_count=0 and version=#{expectedVersion}")
    int acquireExecution(@Param("tenantId") String tenantId, @Param("id") Long id,
                         @Param("expectedVersion") int expectedVersion, @Param("now") Date now);

    @Update("update gl_payment_reconciliation_batch set status='COMPLETED',matched_count=#{matched},discrepancy_count=#{issues},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='RECONCILING' and version=#{leaseVersion}")
    int completeExecution(@Param("tenantId") String tenantId, @Param("id") Long id,
                          @Param("leaseVersion") int leaseVersion, @Param("matched") int matched,
                          @Param("issues") int issues, @Param("now") Date now);

    @Update("update gl_payment_reconciliation_batch set status='FAILED',failure_reason=#{reason},version=version+1,update_time=#{now} where tenant_id=#{tenantId} and id=#{id} and status='RECONCILING'")
    int markExecutionFailed(@Param("tenantId") String tenantId, @Param("id") Long id,
                            @Param("reason") String reason, @Param("now") Date now);
}
