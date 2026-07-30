package com.gameluck.payment.mapper;

import com.gameluck.payment.domain.PaymentSettlementActionLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface PaymentSettlementActionLogMapper {
    @Insert("insert into gl_payment_settlement_action_log (id,tenant_id,batch_id,action_type,before_status,after_status,operator_id,operator_name,remark,evidence_snapshot_json,create_time) values (#{entity.id},#{entity.tenantId},#{entity.batchId},#{entity.actionType},#{entity.beforeStatus},#{entity.afterStatus},#{entity.operatorId},#{entity.operatorName},#{entity.remark},#{entity.evidenceSnapshotJson},COALESCE(#{entity.createTime},CURRENT_TIMESTAMP))")
    int insert(@Param("entity") PaymentSettlementActionLog entity);

    @Select("select * from gl_payment_settlement_action_log where tenant_id=#{tenantId} and batch_id=#{batchId} order by create_time,id")
    List<PaymentSettlementActionLog> selectByBatch(@Param("tenantId") String tenantId,
        @Param("batchId") Long batchId);
}
