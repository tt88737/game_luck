package com.gameluck.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gameluck.payment.domain.SimulatedPaymentDispatch;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SimulatedPaymentDispatchMapper extends BaseMapper<SimulatedPaymentDispatch> {

    @Select("select d.* from gl_simulated_payment_dispatch d inner join gl_payment_webhook_event e on e.tenant_id = d.tenant_id and e.provider_code = 'SIMULATED' and e.provider_session_no = d.provider_session_no and e.provider_event_id = d.provider_event_id and e.status in ('PROCESSED', 'IGNORED') where d.tenant_id = #{tenantId} and d.provider_session_no = #{providerSessionNo} order by d.create_time desc, d.id desc limit 1")
    SimulatedPaymentDispatch selectLatestDelivered(@Param("tenantId") String tenantId,
                                                    @Param("providerSessionNo") String providerSessionNo);

    @Select("select d.* from gl_simulated_payment_dispatch d inner join gl_payment_webhook_event e on e.tenant_id = d.tenant_id and e.provider_code = 'SIMULATED' and e.provider_session_no = d.provider_session_no and e.provider_event_id = d.provider_event_id and e.status in ('FAILED', 'PROCESSED', 'IGNORED') where d.tenant_id = #{tenantId} and d.provider_session_no = #{providerSessionNo} order by d.create_time desc, d.id desc limit 1")
    SimulatedPaymentDispatch selectLatestReplayable(@Param("tenantId") String tenantId,
                                                     @Param("providerSessionNo") String providerSessionNo);

    @Select("select count(*) from gl_payment_webhook_event where tenant_id = #{tenantId} and provider_code = 'SIMULATED' and provider_session_no = #{providerSessionNo} and status = 'PROCESSED' and event_type in ('REFUND_SUCCEEDED', 'CHARGEBACK_CREATED')")
    int countProcessedReversal(@Param("tenantId") String tenantId,
                               @Param("providerSessionNo") String providerSessionNo);
}
