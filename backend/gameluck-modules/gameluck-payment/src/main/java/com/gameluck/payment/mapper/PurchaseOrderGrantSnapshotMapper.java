package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Purchase order grant snapshot mapper.
 */
public interface PurchaseOrderGrantSnapshotMapper extends BaseMapper<PurchaseOrderGrantSnapshot> {

    @Select("select * from gl_purchase_order_grant_snapshot where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} order by id asc")
    List<PurchaseOrderGrantSnapshot> selectByPurchaseOrderNo(@Param("tenantId") String tenantId,
                                                             @Param("purchaseOrderNo") String purchaseOrderNo);

    @Select("select * from gl_purchase_order_grant_snapshot where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} order by id asc for update")
    List<PurchaseOrderGrantSnapshot> selectByPurchaseOrderNoForUpdate(@Param("tenantId") String tenantId,
                                                                      @Param("purchaseOrderNo") String purchaseOrderNo);
}
