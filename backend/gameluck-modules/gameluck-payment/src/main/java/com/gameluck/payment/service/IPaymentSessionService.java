package com.gameluck.payment.service;

import com.gameluck.payment.client.domain.bo.ClientPaymentSessionCreateBo;
import com.gameluck.payment.client.domain.vo.ClientPaymentSessionVo;

public interface IPaymentSessionService {
    ClientPaymentSessionVo create(Long memberId, String orderNo, ClientPaymentSessionCreateBo bo);

    ClientPaymentSessionVo get(Long memberId, String sessionNo);
}
