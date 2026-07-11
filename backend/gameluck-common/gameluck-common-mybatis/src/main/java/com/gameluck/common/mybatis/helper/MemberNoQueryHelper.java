package com.gameluck.common.mybatis.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gameluck.common.core.utils.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MemberNoQueryHelper {

    private MemberNoQueryHelper() {
    }

    public static <T> void apply(LambdaQueryWrapper<T> wrapper, String memberNo, String tableName) {
        wrapper.apply(StringUtils.isNotBlank(memberNo),
            "member_id in (select id from gl_member_profile where member_no = {0} and del_flag = '0' and tenant_id = " + tableName + ".tenant_id)",
            memberNo);
    }

    public static <T> void fillMemberNo(
        JdbcTemplate jdbcTemplate,
        Collection<T> rows,
        Function<T, Long> memberIdGetter,
        BiConsumer<T, String> memberNoSetter
    ) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        java.util.List<Long> memberIds = rows.stream()
            .map(memberIdGetter)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (memberIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(memberIds.size(), "?"));
        String sql = "select id, member_no from gl_member_profile where del_flag = '0' and id in (" + placeholders + ")";
        Map<Long, String> memberNoMap = jdbcTemplate.queryForList(sql, memberIds.toArray()).stream()
            .collect(Collectors.toMap(
                row -> ((Number) row.get("id")).longValue(),
                row -> Objects.toString(row.get("member_no"), ""),
                (left, right) -> left
            ));
        rows.forEach(row -> memberNoSetter.accept(row, memberNoMap.get(memberIdGetter.apply(row))));
    }

    public static <T> T fillMemberNo(
        JdbcTemplate jdbcTemplate,
        T row,
        Function<T, Long> memberIdGetter,
        BiConsumer<T, String> memberNoSetter
    ) {
        if (row != null) {
            fillMemberNo(jdbcTemplate, java.util.List.of(row), memberIdGetter, memberNoSetter);
        }
        return row;
    }
}
