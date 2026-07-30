package com.gameluck.wallet.service;

import com.gameluck.wallet.client.domain.bo.ClientExchangeOrderBo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOrderVo;

/**
 * Wallet exchange order runtime service.
 */
public interface IWalletExchangeOrderService {

    ClientExchangeOrderVo submit(Long memberId, ClientExchangeOrderBo bo);
}
