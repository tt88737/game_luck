package com.gameluck.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.gameluck.common.core.utils.MessageUtils;

/**
 * 用户状态
 *
 * @author gameluck
 */
@Getter
@AllArgsConstructor
public enum UserStatus {
    /**
     * 正常
     */
    OK("0", "user.status.ok"),
    /**
     * 停用
     */
    DISABLE("1", "user.status.disable"),
    /**
     * 删除
     */
    DELETED("2", "user.status.deleted");

    private final String code;
    private final String info;

    public String getInfo() {
        return MessageUtils.message(info);
    }

}
