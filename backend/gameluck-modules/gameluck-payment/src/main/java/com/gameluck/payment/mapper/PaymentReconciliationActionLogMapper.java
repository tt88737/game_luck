package com.gameluck.payment.mapper;

import com.gameluck.payment.domain.PaymentReconciliationActionLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PaymentReconciliationActionLogMapper {
    @Insert("insert into gl_payment_reconciliation_action_log (id,tenant_id,batch_id,issue_id,action_type,before_status,after_status,operator_id,operator_name,remark,create_time) values (#{entity.id},#{entity.tenantId},#{entity.batchId},#{entity.issueId},#{entity.actionType},#{entity.beforeStatus},#{entity.afterStatus},#{entity.operatorId},#{entity.operatorName},#{entity.remark},COALESCE(#{entity.createTime},CURRENT_TIMESTAMP))")
    int insert(@Param("entity") PaymentReconciliationActionLog entity);

    @Select("select * from gl_payment_reconciliation_action_log where tenant_id = #{tenantId} and batch_id = #{batchId} order by create_time,id")
    List<PaymentReconciliationActionLog> selectByBatch(@Param("tenantId") String tenantId,
                                                       @Param("batchId") Long batchId);

    @Select("select * from gl_payment_reconciliation_action_log where tenant_id = #{tenantId} and issue_id = #{issueId} order by create_time,id")
    List<PaymentReconciliationActionLog> selectByIssue(@Param("tenantId") String tenantId,
                                                       @Param("issueId") Long issueId);
}
