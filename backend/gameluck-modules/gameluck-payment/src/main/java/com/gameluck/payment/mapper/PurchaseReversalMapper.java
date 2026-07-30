package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.PurchaseReversal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface PurchaseReversalMapper extends BaseMapper<PurchaseReversal> {

    @Select("select * from gl_purchase_reversal where tenant_id = #{tenantId} and event_key = #{eventKey} limit 1")
    PurchaseReversal selectByEventKey(@Param("tenantId") String tenantId,
                                      @Param("eventKey") String eventKey);

    @Select("select * from gl_purchase_reversal where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} order by create_time desc, id desc limit 1")
    PurchaseReversal selectByPurchaseOrderNo(@Param("tenantId") String tenantId,
                                             @Param("purchaseOrderNo") String purchaseOrderNo);

    @Select("<script>select * from (select r.*,row_number() over(partition by r.purchase_order_no order by r.create_time desc,r.id desc) rn from gl_purchase_reversal r where r.tenant_id=#{tenantId} and r.purchase_order_no in <foreach collection='orderNos' item='v' open='(' separator=',' close=')'>#{v}</foreach>) q where q.rn=1</script>")
    List<PurchaseReversal> selectReconciliationLatestCandidates(@Param("tenantId") String tenantId,
        @Param("orderNos") List<String> orderNos);

    @Select("select * from gl_purchase_reversal where tenant_id = #{tenantId} and reversal_no = #{reversalNo} limit 1")
    PurchaseReversal selectByReversalNo(@Param("tenantId") String tenantId,
                                        @Param("reversalNo") String reversalNo);

    @Select("select * from gl_purchase_reversal where tenant_id = #{tenantId} and reversal_no = #{reversalNo} limit 1 for update")
    PurchaseReversal selectByReversalNoForUpdate(@Param("tenantId") String tenantId,
                                                 @Param("reversalNo") String reversalNo);

    @Update("update gl_purchase_reversal set status = #{status}, disposition_status = #{afterStatus}, reviewed_by = #{reviewedBy}, reviewed_name = #{reviewedName}, review_note = #{reviewNote}, resolved_time = #{resolvedTime}, completed_time = #{completedTime}, review_reason = #{reviewReason}, retry_count = #{retryCount}, last_retry_time = #{lastRetryTime}, update_time = #{resolvedTime}, version = version + 1 where tenant_id = #{tenantId} and reversal_no = #{reversalNo} and disposition_status = #{beforeStatus}")
    int finalizeDisposition(@Param("tenantId") String tenantId, @Param("reversalNo") String reversalNo,
                            @Param("beforeStatus") String beforeStatus, @Param("afterStatus") String afterStatus,
                            @Param("status") String status, @Param("reviewedBy") Long reviewedBy,
                            @Param("reviewedName") String reviewedName, @Param("reviewNote") String reviewNote,
                            @Param("resolvedTime") java.util.Date resolvedTime,
                            @Param("completedTime") java.util.Date completedTime,
                            @Param("reviewReason") String reviewReason,
                            @Param("retryCount") Integer retryCount,
                            @Param("lastRetryTime") java.util.Date lastRetryTime);
}
