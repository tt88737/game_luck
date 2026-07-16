package com.gameluck.payment.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.payment.domain.PurchaseOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Purchase order mapper.
 */
public interface PurchaseOrderMapper extends BaseMapperPlus<PurchaseOrder, PurchaseOrder> {

    @Select("select * from gl_purchase_order where tenant_id = #{tenantId} and idempotency_key = #{idempotencyKey} limit 1")
    PurchaseOrder selectByIdempotencyKey(@Param("tenantId") String tenantId, @Param("idempotencyKey") String idempotencyKey);
}
