package com.gameluck.game.service;

import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.game.domain.bo.GameBetOrderBo;
import com.gameluck.game.domain.vo.GameBetOrderVo;

public interface IGameBetOrderService {

    TableDataInfo<GameBetOrderVo> queryPageList(GameBetOrderBo bo, PageQuery pageQuery);

    GameBetOrderVo queryById(Long id);

    Boolean insertByBo(GameBetOrderBo bo);

    GameBetOrderVo placeBet(Long id);

    GameBetOrderVo settle(Long id);

    GameBetOrderVo cancel(Long id);
}
