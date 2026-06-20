package com.tangluck.lobby;

import com.tangluck.admin.AdminAuditService;
import com.tangluck.admin.AdminOperatorContext;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.promotion.PromotionCampaign;
import com.tangluck.promotion.PromotionCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.tangluck.lobby.LobbyDtos.LobbyCampaignDto;
import static com.tangluck.lobby.LobbyDtos.LobbyCardDto;
import static com.tangluck.lobby.LobbyDtos.LobbyResponse;
import static com.tangluck.lobby.LobbyDtos.LobbyTaskDto;
import static com.tangluck.lobby.LobbyDtos.UpdateLobbyCardRequest;

@Service
public class LobbyService {
    private final LobbyCardRepository cardRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final AdminAuditService auditService;
    private final Clock clock;

    public LobbyService(LobbyCardRepository cardRepository, PromotionCampaignRepository campaignRepository, AdminAuditService auditService) {
        this.cardRepository = cardRepository;
        this.campaignRepository = campaignRepository;
        this.auditService = auditService;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public LobbyResponse publicLobby() {
        var cards = cardRepository.findByStatusOrderBySortOrderAsc("active").stream().map(this::toCardDto).toList();
        var activeCampaigns = campaignRepository.findAll().stream()
                .filter(campaign -> "active".equals(campaign.getStatus()))
                .sorted(Comparator.comparing(PromotionCampaign::getCampaignCode))
                .toList();
        var campaigns = activeCampaigns.stream()
                .filter(campaign -> !"daily_task".equals(campaign.getCampaignType()))
                .map(campaign -> new LobbyCampaignDto(campaign.getCampaignCode(), campaign.getCampaignType(), campaign.getStatus()))
                .toList();
        var tasks = activeCampaigns.stream()
                .filter(campaign -> "daily_task".equals(campaign.getCampaignType()))
                .map(campaign -> new LobbyTaskDto(campaign.getCampaignCode(), campaign.getCampaignCode(), 1, "in_progress"))
                .toList();
        return new LobbyResponse(cards, campaigns, tasks);
    }

    @Transactional(readOnly = true)
    public List<LobbyCardDto> adminCards() {
        return cardRepository.findAll().stream()
                .sorted(Comparator.comparing(LobbyCard::getSortOrder))
                .map(this::toCardDto)
                .toList();
    }

    @Transactional
    public LobbyCardDto updateCard(String cardCode, UpdateLobbyCardRequest request, AdminOperatorContext operator) {
        var card = cardRepository.findByCardCode(cardCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Lobby card does not exist.", Map.of("card_code", cardCode)));
        var before = cardJson(card);
        card.update(request.title(), request.subtitle(), request.imageUrl(), request.targetUrl(), request.status(), request.sortOrder(), clock.instant());
        var saved = cardRepository.save(card);
        auditService.write(operator, "lobby_card_update", "lobby_card", cardCode, before, cardJson(saved), null);
        return toCardDto(saved);
    }

    private LobbyCardDto toCardDto(LobbyCard card) {
        return new LobbyCardDto(card.getCardCode(), card.getTitle(), card.getSubtitle(), card.getImageUrl(), card.getTargetUrl(), card.getStatus(), card.getSortOrder());
    }

    private String cardJson(LobbyCard card) {
        return "{\"status\":\"" + card.getStatus() + "\",\"sortOrder\":" + card.getSortOrder() + "}";
    }
}
