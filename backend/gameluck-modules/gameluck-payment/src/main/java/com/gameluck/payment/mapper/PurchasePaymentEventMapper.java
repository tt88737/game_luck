package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PurchasePaymentEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PurchasePaymentEventMapper extends BaseMapper<PurchasePaymentEvent> {

    @Select("select * from gl_purchase_payment_event where tenant_id = #{tenantId} and event_key = #{eventKey} limit 1")
    PurchasePaymentEvent selectByEventKey(@Param("tenantId") String tenantId, @Param("eventKey") String eventKey);

    @Select("select * from gl_purchase_payment_event where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} order by create_time asc, id asc")
    List<PurchasePaymentEvent> selectByPurchaseOrderNo(@Param("tenantId") String tenantId,
                                                       @Param("purchaseOrderNo") String purchaseOrderNo);

    @Select("<script>select * from (select e.*,row_number() over(partition by e.purchase_order_no order by e.create_time desc,e.id desc) rn from gl_purchase_payment_event e where e.tenant_id=#{tenantId} and e.purchase_order_no in <foreach collection='orderNos' item='v' open='(' separator=',' close=')'>#{v}</foreach>) q where q.rn=1</script>")
    List<PurchasePaymentEvent> selectReconciliationLatestCandidates(@Param("tenantId") String tenantId,
        @Param("orderNos") List<String> orderNos);
}
