package com.gameluck.wallet.client.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.gameluck.common.core.domain.R;
import com.gameluck.wallet.client.domain.bo.ClientExchangeOrderBo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOrderVo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOptionVo;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletCurrencyVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.client.service.ClientWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@SaIgnore
@RestController
@RequestMapping("/api/client/wallet")
public class ClientWalletController {

    private final ClientWalletService clientWalletService;

    @GetMapping("/accounts")
    public R<List<ClientWalletAccountVo>> accounts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientWalletService.accounts(authorization));
    }

    @GetMapping("/currencies")
    public R<List<ClientWalletCurrencyVo>> currencies(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestHeader(value = "X-Client-Channel", required = false, defaultValue = "H5") String channel) {
        return R.ok(clientWalletService.currencies(authorization, channel));
    }

    @GetMapping("/exchange/options")
    public R<List<ClientExchangeOptionVo>> exchangeOptions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                           @RequestHeader(value = "X-Client-Channel", required = false, defaultValue = "H5") String channel) {
        return R.ok(clientWalletService.exchangeOptions(authorization, channel));
    }

    @PostMapping("/exchange/orders")
    public R<ClientExchangeOrderVo> exchangeOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @Valid @RequestBody ClientExchangeOrderBo bo) {
        return R.ok(clientWalletService.exchangeOrder(authorization, bo));
    }

    @GetMapping("/ledgers")
    public R<ClientPageVo<ClientWalletLedgerVo>> ledgers(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestParam(required = false) String currencyCode,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(clientWalletService.ledgers(authorization, currencyCode, pageNum, pageSize));
    }
}
