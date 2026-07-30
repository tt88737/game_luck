package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PurchaseReversalReviewLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PurchaseReversalReviewLogMapper extends BaseMapper<PurchaseReversalReviewLog> {

    @Select("select * from gl_purchase_reversal_review_log where tenant_id = #{tenantId} and request_key = #{requestKey} limit 1")
    PurchaseReversalReviewLog selectByRequestKey(@Param("tenantId") String tenantId,
                                                 @Param("requestKey") String requestKey);

    @Select("select * from gl_purchase_reversal_review_log where tenant_id = #{tenantId} and reversal_no = #{reversalNo} order by create_time asc, id asc")
    List<PurchaseReversalReviewLog> selectByReversalNo(@Param("tenantId") String tenantId,
                                                       @Param("reversalNo") String reversalNo);
}
