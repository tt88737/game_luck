package com.gameluck.member.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.member.domain.bo.MemberProfileBo;
import com.gameluck.member.domain.vo.MemberProfileVo;
import com.gameluck.member.service.IMemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Member profile admin controller.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/member/profile")
public class MemberProfileController extends BaseController {

    private final IMemberProfileService memberProfileService;

    @SaCheckPermission("member:profile:list")
    @GetMapping("/list")
    public TableDataInfo<MemberProfileVo> list(MemberProfileBo bo, PageQuery pageQuery) {
        return memberProfileService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("member:profile:query")
    @GetMapping("/{id}")
    public R<MemberProfileVo> getInfo(@PathVariable Long id) {
        return R.ok(memberProfileService.queryById(id));
    }

    @SaCheckPermission("member:profile:add")
    @Log(title = "Member profile add", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody MemberProfileBo bo) {
        return toAjax(memberProfileService.insertByBo(bo));
    }

    @SaCheckPermission("member:profile:edit")
    @Log(title = "Member profile edit", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody MemberProfileBo bo) {
        return toAjax(memberProfileService.updateByBo(bo));
    }

    @SaCheckPermission("member:profile:edit")
    @Log(title = "Member profile status", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/status/{status}")
    public R<MemberProfileVo> updateStatus(@PathVariable Long id, @PathVariable String status) {
        return R.ok(memberProfileService.updateStatus(id, status));
    }

    @SaCheckPermission("member:profile:remove")
    @Log(title = "Member profile remove", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(memberProfileService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
