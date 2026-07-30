package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.gameluck.payment.domain.PaymentSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.payment.domain.bo.PaymentSessionAdminBo;
import com.gameluck.payment.domain.vo.PaymentSessionAdminVo;

public interface PaymentSessionMapper extends BaseMapper<PaymentSession> {

    @Select("<script>select s.id,s.session_no,s.purchase_order_id,s.purchase_order_no,s.member_id,m.member_no,s.provider_code,s.provider_session_no,s.pay_currency_code,s.pay_amount,s.checkout_url,s.status,s.expire_time,s.completed_time,s.create_time,s.update_time from gl_payment_session s left join gl_member_profile m on m.tenant_id=s.tenant_id and m.id=s.member_id where s.tenant_id=#{tenantId}<if test='bo.sessionNo != null and bo.sessionNo != \"\"'> and s.session_no=#{bo.sessionNo}</if><if test='bo.purchaseOrderNo != null and bo.purchaseOrderNo != \"\"'> and s.purchase_order_no=#{bo.purchaseOrderNo}</if><if test='bo.providerSessionNo != null and bo.providerSessionNo != \"\"'> and s.provider_session_no=#{bo.providerSessionNo}</if><if test='bo.memberId != null'> and s.member_id=#{bo.memberId}</if><if test='bo.memberNo != null and bo.memberNo != \"\"'> and m.member_no=#{bo.memberNo}</if><if test='bo.providerCode != null and bo.providerCode != \"\"'> and s.provider_code=#{bo.providerCode}</if><if test='bo.status != null and bo.status != \"\"'> and s.status=#{bo.status}</if><if test='bo.payCurrencyCode != null and bo.payCurrencyCode != \"\"'> and s.pay_currency_code=#{bo.payCurrencyCode}</if><if test='bo.beginTime != null'> and s.create_time &gt;=#{bo.beginTime}</if><if test='bo.endTime != null'> and s.create_time &lt;=#{bo.endTime}</if> order by s.create_time desc,s.id desc</script>")
    Page<PaymentSessionAdminVo> selectAdminPage(Page<PaymentSessionAdminVo> page, @Param("tenantId") String tenantId,
                                                @Param("bo") PaymentSessionAdminBo bo);

    @Select("select s.id,s.session_no,s.purchase_order_id,s.purchase_order_no,s.member_id,m.member_no,s.provider_code,s.provider_session_no,s.pay_currency_code,s.pay_amount,s.checkout_url,s.status,s.expire_time,s.completed_time,s.create_time,s.update_time from gl_payment_session s left join gl_member_profile m on m.tenant_id=s.tenant_id and m.id=s.member_id where s.tenant_id=#{tenantId} and s.id=#{id} limit 1")
    PaymentSessionAdminVo selectAdminById(@Param("tenantId") String tenantId, @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("select * from gl_payment_session where provider_code = #{providerCode} and provider_session_no = #{providerSessionNo} order by id limit 2")
    List<PaymentSession> selectPublicByProviderSessionNo(@Param("providerCode") String providerCode,
                                                         @Param("providerSessionNo") String providerSessionNo);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and request_key = #{requestKey} limit 1")
    PaymentSession selectByRequestKey(@Param("tenantId") String tenantId, @Param("requestKey") String requestKey);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and request_key = #{requestKey} limit 1 for update")
    PaymentSession selectByRequestKeyForUpdate(@Param("tenantId") String tenantId, @Param("requestKey") String requestKey);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and session_no = #{sessionNo} limit 1")
    PaymentSession selectBySessionNo(@Param("tenantId") String tenantId, @Param("sessionNo") String sessionNo);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and session_no = #{sessionNo} limit 1 for update")
    PaymentSession selectBySessionNoForUpdate(@Param("tenantId") String tenantId,
                                              @Param("sessionNo") String sessionNo);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and purchase_order_no = #{purchaseOrderNo} and status in ('CREATED', 'PENDING') and expire_time > #{now} order by create_time desc, id desc limit 1 for update")
    PaymentSession selectActiveByOrderNoForUpdate(@Param("tenantId") String tenantId,
                                                   @Param("purchaseOrderNo") String purchaseOrderNo,
                                                   @Param("now") Date now);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and provider_code = #{providerCode} and provider_session_no = #{providerSessionNo} limit 1 for update")
    PaymentSession selectByProviderSessionNoForUpdate(@Param("tenantId") String tenantId,
                                                       @Param("providerCode") String providerCode,
                                                       @Param("providerSessionNo") String providerSessionNo);

    @Select("select * from gl_payment_session where tenant_id = #{tenantId} and provider_code = #{providerCode} and provider_session_no = #{providerSessionNo} limit 1")
    PaymentSession selectByProviderSessionNo(@Param("tenantId") String tenantId,
                                              @Param("providerCode") String providerCode,
                                              @Param("providerSessionNo") String providerSessionNo);

    @Select("select * from gl_payment_session where tenant_id=#{tenantId} and provider_code=#{providerCode} and provider_session_no=#{providerSessionNo} order by id limit 2")
    List<PaymentSession> selectReconciliationCandidates(@Param("tenantId") String tenantId,
        @Param("providerCode") String providerCode, @Param("providerSessionNo") String providerSessionNo);

    @Select("<script>select * from (select s.*,row_number() over(partition by s.provider_session_no order by s.id) rn from gl_payment_session s where s.tenant_id=#{tenantId} and s.provider_code=#{providerCode} and s.provider_session_no in <foreach collection='providerSessionNos' item='v' open='(' separator=',' close=')'>#{v}</foreach>) q where q.rn &lt;= 2 order by provider_session_no,id</script>")
    List<PaymentSession> selectReconciliationCandidatesBatch(@Param("tenantId") String tenantId,
        @Param("providerCode") String providerCode, @Param("providerSessionNos") List<String> providerSessionNos);

    @Update("update gl_payment_session set status = #{newStatus}, completed_time = #{completedTime}, version = version + 1, update_time = #{completedTime} where tenant_id = #{tenantId} and id = #{id} and status = #{expectedStatus}")
    int updateStatusGuarded(@Param("tenantId") String tenantId, @Param("id") Long id,
                            @Param("newStatus") String newStatus, @Param("expectedStatus") String expectedStatus,
                            @Param("completedTime") Date completedTime);
}
