package com.gameluck.payment.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.payment.domain.PurchaseOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * Purchase order mapper.
 */
public interface PurchaseOrderMapper extends BaseMapperPlus<PurchaseOrder, PurchaseOrder> {

    @Select("select * from gl_purchase_order where tenant_id = #{tenantId} and idempotency_key = #{idempotencyKey} limit 1")
    PurchaseOrder selectByIdempotencyKey(@Param("tenantId") String tenantId, @Param("idempotencyKey") String idempotencyKey);

    @Select("select * from gl_purchase_order where tenant_id = #{tenantId} and idempotency_key = #{idempotencyKey} limit 1 for update")
    PurchaseOrder selectByIdempotencyKeyForUpdate(@Param("tenantId") String tenantId, @Param("idempotencyKey") String idempotencyKey);

    @Select("select * from gl_purchase_order where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} limit 1 for update")
    PurchaseOrder selectByOrderNoForUpdate(@Param("tenantId") String tenantId, @Param("purchaseOrderNo") String purchaseOrderNo);

    @Select("select * from gl_purchase_order where tenant_id=#{tenantId} and purchase_order_no=#{purchaseOrderNo} limit 1")
    PurchaseOrder selectByOrderNo(@Param("tenantId") String tenantId, @Param("purchaseOrderNo") String purchaseOrderNo);

    @Select("<script>select * from gl_purchase_order where tenant_id=#{tenantId} and purchase_order_no in <foreach collection='orderNos' item='v' open='(' separator=',' close=')'>#{v}</foreach></script>")
    List<PurchaseOrder> selectReconciliationByOrderNos(@Param("tenantId") String tenantId,
        @Param("orderNos") List<String> orderNos);

    @Select("select count(1) from gl_purchase_order where tenant_id = #{tenantId} and member_id = #{memberId} and status = 'CREDITED'")
    long countCreditedByMember(@Param("tenantId") String tenantId, @Param("memberId") Long memberId);

    @Select("select count(1) from gl_purchase_order where tenant_id = #{tenantId} and member_id = #{memberId} and offer_id = #{offerId} and status = 'CREDITED'")
    long countCreditedByMemberAndOffer(@Param("tenantId") String tenantId, @Param("memberId") Long memberId,
                                       @Param("offerId") Long offerId);

    @Select("select count(1) from gl_purchase_order where tenant_id = #{tenantId} and member_id = #{memberId} and offer_id = #{offerId} and status = 'CREDITED' and credited_time >= #{dayStart} and credited_time < #{nextDayStart}")
    long countCreditedByMemberOfferAndCreditedTimeRange(@Param("tenantId") String tenantId, @Param("memberId") Long memberId,
                                                        @Param("offerId") Long offerId, @Param("dayStart") Date dayStart,
                                                        @Param("nextDayStart") Date nextDayStart);
}
