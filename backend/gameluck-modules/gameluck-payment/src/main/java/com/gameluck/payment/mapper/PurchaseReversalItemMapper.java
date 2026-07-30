package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PurchaseReversalItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PurchaseReversalItemMapper extends BaseMapper<PurchaseReversalItem> {

    @Select("select * from gl_purchase_reversal_item where tenant_id = #{tenantId} and reversal_no = #{reversalNo} order by currency_code asc, id asc")
    List<PurchaseReversalItem> selectByReversalNo(@Param("tenantId") String tenantId,
                                                  @Param("reversalNo") String reversalNo);
}
