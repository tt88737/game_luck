package com.tangluck.slots;

import com.tangluck.admin.AdminOperatorContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tangluck.slots.SlotDtos.SlotGameDto;
import static com.tangluck.slots.SlotDtos.SlotRoundDto;
import static com.tangluck.slots.SlotDtos.SlotRoundPage;
import static com.tangluck.slots.SlotDtos.SpinRequest;
import static com.tangluck.slots.SlotDtos.UpdateSlotGameRequest;

@RestController
@RequestMapping("/api/v1")
public class SlotController {
    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/slots/games")
    public List<SlotGameDto> activeGames() {
        return slotService.activeGames();
    }

    @PostMapping("/slots/{gameCode}/spin")
    public SlotRoundDto spin(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable String gameCode,
            @Valid @RequestBody SpinRequest request
    ) {
        return slotService.spin(userId, gameCode, idempotencyKey, request);
    }

    @GetMapping("/slots/rounds")
    public SlotRoundPage playerRounds(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        return slotService.playerRounds(userId, page, pageSize);
    }

    @GetMapping("/admin/games")
    public List<SlotGameDto> adminGames(HttpServletRequest request) {
        var operator = AdminOperatorContext.from(request);
        operator.require("game.read");
        return slotService.adminGames();
    }

    @PatchMapping("/admin/games/{gameCode}")
    public SlotGameDto updateGame(
            HttpServletRequest request,
            @PathVariable String gameCode,
            @Valid @RequestBody UpdateSlotGameRequest updateRequest
    ) {
        var operator = AdminOperatorContext.from(request);
        operator.require("game.write");
        return slotService.updateGame(gameCode, updateRequest, operator);
    }

    @GetMapping("/admin/game-rounds")
    public SlotRoundPage adminRounds(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int pageSize
    ) {
        var operator = AdminOperatorContext.from(request);
        operator.require("game.round.read");
        return slotService.adminRounds(page, pageSize);
    }
}
