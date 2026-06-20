package com.tangluck.lobby;

import java.util.List;

public final class LobbyDtos {
    private LobbyDtos() {
    }

    public record LobbyCardDto(
            String cardCode,
            String title,
            String subtitle,
            String imageUrl,
            String targetUrl,
            String status,
            int sortOrder
    ) {
    }

    public record UpdateLobbyCardRequest(
            String title,
            String subtitle,
            String imageUrl,
            String targetUrl,
            String status,
            int sortOrder
    ) {
    }

    public record LobbyCampaignDto(String campaignCode, String campaignType, String status) {
    }

    public record LobbyTaskDto(String taskId, String taskCode, int target, String status) {
    }

    public record LobbyResponse(
            List<LobbyCardDto> cards,
            List<LobbyCampaignDto> campaigns,
            List<LobbyTaskDto> tasks
    ) {
    }
}
