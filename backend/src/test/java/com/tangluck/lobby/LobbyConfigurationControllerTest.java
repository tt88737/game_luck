package com.tangluck.lobby;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LobbyConfigurationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicLobbyReturnsActiveConfiguredCardsAndCampaigns() throws Exception {
        mockMvc.perform(get("/api/v1/lobby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[?(@.cardCode == 'slots_main' && @.status == 'active')]").exists())
                .andExpect(jsonPath("$.cards[?(@.cardCode == 'table_games_locked')]").doesNotExist())
                .andExpect(jsonPath("$.campaigns[?(@.campaignCode == 'register_bonus_v1' && @.status == 'active')]").exists())
                .andExpect(jsonPath("$.tasks[?(@.taskCode == 'view_rules_task_v1')]").exists());
    }

    @Test
    void adminUpdatesLobbyCardAndWritesAudit() throws Exception {
        mockMvc.perform(get("/api/v1/admin/lobby-cards")
                        .header("X-Admin-Permissions", "lobby.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cardCode == 'slots_main')]").exists());

        mockMvc.perform(patch("/api/v1/admin/lobby-cards/slots_main")
                        .header("X-Admin-Operator-Id", "31")
                        .header("X-Admin-Operator-Role", "content_admin")
                        .header("X-Admin-Permissions", "lobby.write")
                        .header("X-Forwarded-For", "203.0.113.31")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lucky Slots Live",
                                  "subtitle": "GC play with configured rewards",
                                  "imageUrl": "/assets/lobby/slots-live.png",
                                  "targetUrl": "/app/activity",
                                  "status": "paused",
                                  "sortOrder": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lucky Slots Live"))
                .andExpect(jsonPath("$.status").value("paused"));

        mockMvc.perform(get("/api/v1/lobby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[?(@.cardCode == 'slots_main')]").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "lobby_card")
                        .param("target_id", "slots_main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'lobby_card_update' && @.operatorId == 31 && @.operatorRole == 'content_admin' && @.ip == '203.0.113.31')]").exists());
    }

    @Test
    void adminCampaignsReturnsRealCampaignList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/campaigns")
                        .header("X-Admin-Permissions", "campaign.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.campaignCode == 'register_bonus_v1' && @.legalApprovalId == 'LEGAL-2026-0617-CA')]").exists())
                .andExpect(jsonPath("$[?(@.campaignCode == 'OPS_SC_BONUS')]").doesNotExist());
    }
}
