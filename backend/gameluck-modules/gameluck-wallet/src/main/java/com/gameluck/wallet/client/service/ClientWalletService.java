package com.gameluck.wallet.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.wallet.client.domain.bo.ClientExchangeOrderBo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOrderVo;
import com.gameluck.wallet.client.domain.vo.ClientExchangeOptionVo;
import com.gameluck.wallet.client.domain.vo.ClientPageVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletCurrencyVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletAccountVo;
import com.gameluck.wallet.client.domain.vo.ClientWalletLedgerVo;
import com.gameluck.wallet.domain.WalletAccount;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.mapper.WalletAccountMapper;
import com.gameluck.wallet.mapper.WalletTransactionMapper;
import com.gameluck.wallet.service.IWalletCurrencyPolicyService;
import com.gameluck.wallet.service.IWalletExchangeOrderService;
import com.gameluck.wallet.service.IWalletExchangeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClientWalletService {

    private static final String TENANT_ID = "000000";

    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final IWalletCurrencyPolicyService walletCurrencyPolicyService;
    private final IWalletExchangeRuleService walletExchangeRuleService;
    private final IWalletExchangeOrderService walletExchangeOrderService;
    private final ClientTokenService clientTokenService;

    public List<ClientWalletAccountVo> accounts(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        Map<String, ClientWalletCurrencyVo> visibleCurrencies = visibleCurrencyMap(memberId, "H5");
        return walletAccountMapper.selectClientAccounts(TENANT_ID, memberId).stream()
            .filter(account -> visibleCurrencies.containsKey(account.getCurrencyCode()))
            .map(account -> toAccount(account, visibleCurrencies.get(account.getCurrencyCode())))
            .toList();
    }

    public List<ClientWalletCurrencyVo> currencies(String authorization, String channel) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return walletCurrencyPolicyService.listClientCurrencies(TENANT_ID, memberId, channel);
    }

    public List<ClientExchangeOptionVo> exchangeOptions(String authorization, String channel) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return walletExchangeRuleService.listOptions(memberId, channel);
    }

    public ClientExchangeOrderVo exchangeOrder(String authorization, ClientExchangeOrderBo bo) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        return walletExchangeOrderService.submit(memberId, bo);
    }

    public ClientPageVo<ClientWalletLedgerVo> ledgers(String authorization, String currencyCode, Integer pageNum, Integer pageSize) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        List<String> visibleCurrencyCodes = visibleCurrencyMap(memberId, "H5").keySet().stream().toList();
        if (visibleCurrencyCodes.isEmpty()) {
            return emptyLedgerPage();
        }
        String requestedCurrencyCode = normalizeCurrencyCode(currencyCode);
        if (requestedCurrencyCode != null && !visibleCurrencyCodes.contains(requestedCurrencyCode)) {
            return emptyLedgerPage();
        }
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        int offset = (safePageNum - 1) * safePageSize;
        ClientPageVo<ClientWalletLedgerVo> page = new ClientPageVo<>();
        page.setRecords(walletTransactionMapper.selectClientLedgers(TENANT_ID, memberId, requestedCurrencyCode, visibleCurrencyCodes, offset, safePageSize).stream()
            .map(this::toLedger)
            .toList());
        page.setTotal(walletTransactionMapper.countClientLedgers(TENANT_ID, memberId, requestedCurrencyCode, visibleCurrencyCodes));
        return page;
    }

    private ClientWalletAccountVo toAccount(WalletAccount account, ClientWalletCurrencyVo currency) {
        ClientWalletAccountVo vo = new ClientWalletAccountVo();
        vo.setCurrencyCode(account.getCurrencyCode());
        vo.setCurrencyName(currency.getCurrencyName());
        vo.setAvailableBalance(account.getAvailableBalance().setScale(2).toPlainString());
        vo.setFrozenBalance(account.getFrozenBalance().setScale(2).toPlainString());
        vo.setDecimalScale(currency.getDecimalScale());
        vo.setPlayable(currency.getPlayEnabled());
        vo.setWithdrawable(currency.getWithdrawEnabled());
        return vo;
    }

    private Map<String, ClientWalletCurrencyVo> visibleCurrencyMap(Long memberId, String channel) {
        return walletCurrencyPolicyService.listClientCurrencies(TENANT_ID, memberId, channel).stream()
            .collect(Collectors.toMap(ClientWalletCurrencyVo::getCurrencyCode, Function.identity()));
    }

    private ClientPageVo<ClientWalletLedgerVo> emptyLedgerPage() {
        ClientPageVo<ClientWalletLedgerVo> page = new ClientPageVo<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);
        return page;
    }

    private String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return null;
        }
        return currencyCode.trim();
    }

    private ClientWalletLedgerVo toLedger(WalletTransaction transaction) {
        ClientWalletLedgerVo vo = new ClientWalletLedgerVo();
        vo.setLedgerId(transaction.getId());
        vo.setCurrencyCode(transaction.getCurrencyCode());
        vo.setDirection(transaction.getOperation());
        vo.setAmount(transaction.getAmount().setScale(2).toPlainString());
        vo.setAfterAvailable(transaction.getBalanceAfter().setScale(2).toPlainString());
        vo.setBizType(transaction.getSourceType());
        vo.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(transaction.getCreateTime()));
        return vo;
    }
}
