package com.gameluck.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.game.domain.bo.GameBetOrderBo;
import com.gameluck.game.domain.vo.GameBetOrderVo;
import com.gameluck.game.service.IGameBetOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/game/bet")
public class GameBetOrderController extends BaseController {

    private final IGameBetOrderService gameBetOrderService;

    @SaCheckPermission("game:bet:list")
    @GetMapping("/list")
    public TableDataInfo<GameBetOrderVo> list(GameBetOrderBo bo, PageQuery pageQuery) {
        return gameBetOrderService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("game:bet:query")
    @GetMapping("/{id}")
    public R<GameBetOrderVo> getInfo(@PathVariable Long id) {
        return R.ok(gameBetOrderService.queryById(id));
    }

    @SaCheckPermission("game:bet:add")
    @Log(title = "模拟下注订单新增", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody GameBetOrderBo bo) {
        return toAjax(gameBetOrderService.insertByBo(bo));
    }

    @SaCheckPermission("game:bet:place")
    @Log(title = "模拟下注扣款", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/place")
    public R<GameBetOrderVo> place(@PathVariable Long id) {
        return R.ok(gameBetOrderService.placeBet(id));
    }

    @SaCheckPermission("game:bet:settle")
    @Log(title = "模拟结算派彩", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/settle")
    public R<GameBetOrderVo> settle(@PathVariable Long id) {
        return R.ok(gameBetOrderService.settle(id));
    }

    @SaCheckPermission("game:bet:cancel")
    @Log(title = "模拟下注取消退款", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public R<GameBetOrderVo> cancel(@PathVariable Long id) {
        return R.ok(gameBetOrderService.cancel(id));
    }
}
