package com.gameluck.payment.mapper;

import com.gameluck.payment.domain.PaymentSettlementPayoutActionLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PaymentSettlementPayoutActionLogMapper {
    @Insert("insert into gl_payment_settlement_payout_action_log (id,tenant_id,payout_id,action_type,before_status,after_status,operator_id,operator_name,reason,evidence_snapshot_json,expected_version,result_version,create_time) values (#{entity.id},#{entity.tenantId},#{entity.payoutId},#{entity.actionType},#{entity.beforeStatus},#{entity.afterStatus},#{entity.operatorId},#{entity.operatorName},#{entity.reason},#{entity.evidenceSnapshotJson},#{entity.expectedVersion},#{entity.resultVersion},COALESCE(#{entity.createTime},CURRENT_TIMESTAMP))")
    int insert(@Param("entity") PaymentSettlementPayoutActionLog entity);

    @Select("select * from gl_payment_settlement_payout_action_log where tenant_id=#{tenantId} and payout_id=#{payoutId} order by create_time,id")
    List<PaymentSettlementPayoutActionLog> selectByPayout(@Param("tenantId") String tenantId,
        @Param("payoutId") Long payoutId);
}
