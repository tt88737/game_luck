package com.gameluck.wallet.client.controller;

import com.gameluck.common.core.domain.R;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.client.service.ClientWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/client/wallet")
public class ClientWalletController {

    private final ClientWalletService clientWalletService;

    @GetMapping("/accounts")
    public R<List<ClientWalletAccountVo>> accounts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(clientWalletService.accounts(authorization));
    }

    @GetMapping("/ledgers")
    public R<ClientPageVo<ClientWalletLedgerVo>> ledgers(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestParam(required = false) String currencyCode,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(clientWalletService.ledgers(authorization, currencyCode, pageNum, pageSize));
    }
}
