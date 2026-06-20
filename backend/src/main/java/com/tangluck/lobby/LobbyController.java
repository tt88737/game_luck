package com.tangluck.lobby;

import com.tangluck.admin.AdminOperatorContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tangluck.lobby.LobbyDtos.LobbyCardDto;
import static com.tangluck.lobby.LobbyDtos.LobbyResponse;
import static com.tangluck.lobby.LobbyDtos.UpdateLobbyCardRequest;

@RestController
@RequestMapping("/api/v1")
public class LobbyController {
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping("/lobby")
    public LobbyResponse lobby() {
        return lobbyService.publicLobby();
    }

    @GetMapping("/admin/lobby-cards")
    public List<LobbyCardDto> adminCards(HttpServletRequest servletRequest) {
        AdminOperatorContext.from(servletRequest).require("lobby.read");
        return lobbyService.adminCards();
    }

    @PatchMapping("/admin/lobby-cards/{cardCode}")
    public LobbyCardDto updateCard(@PathVariable String cardCode, @RequestBody UpdateLobbyCardRequest request, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("lobby.write");
        return lobbyService.updateCard(cardCode, request, operator);
    }
}
