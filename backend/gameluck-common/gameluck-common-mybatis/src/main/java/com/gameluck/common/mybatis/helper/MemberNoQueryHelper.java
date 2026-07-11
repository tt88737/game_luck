package com.gameluck.common.mybatis.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gameluck.common.core.utils.StringUtils;

public final class MemberNoQueryHelper {

    private MemberNoQueryHelper() {
    }

    public static <T> void apply(LambdaQueryWrapper<T> wrapper, String memberNo, String tableName) {
        wrapper.apply(StringUtils.isNotBlank(memberNo),
            "member_id in (select id from gl_member_profile where member_no = {0} and del_flag = '0' and tenant_id = " + tableName + ".tenant_id)",
            memberNo);
    }
}
