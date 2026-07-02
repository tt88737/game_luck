package com.gameluck.game.mapper;

import com.gameluck.common.mybatis.core.mapper.BaseMapperPlus;
import com.gameluck.game.domain.GameBetOrder;
import com.gameluck.game.domain.vo.GameBetOrderVo;
import org.apache.ibatis.annotations.Param;

public interface GameBetOrderMapper extends BaseMapperPlus<GameBetOrder, GameBetOrderVo> {

    GameBetOrder selectByIdForUpdate(@Param("id") Long id);
}
