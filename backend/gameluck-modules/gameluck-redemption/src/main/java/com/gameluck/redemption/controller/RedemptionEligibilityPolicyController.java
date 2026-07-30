package com.gameluck.redemption.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.log.annotation.Log;
import com.gameluck.common.log.enums.BusinessType;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.web.core.BaseController;
import com.gameluck.redemption.domain.bo.RedemptionEligibilityPolicyBo;
import com.gameluck.redemption.domain.vo.RedemptionEligibilityPolicyVo;
import com.gameluck.redemption.service.IRedemptionEligibilityPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/redemption/eligibility-policy")
public class RedemptionEligibilityPolicyController extends BaseController {

    private final IRedemptionEligibilityPolicyService redemptionEligibilityPolicyService;

    @SaCheckPermission("redemption:eligibilityPolicy:list")
    @GetMapping("/list")
    public TableDataInfo<RedemptionEligibilityPolicyVo> list(RedemptionEligibilityPolicyBo bo, PageQuery pageQuery) {
        return redemptionEligibilityPolicyService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("redemption:eligibilityPolicy:query")
    @GetMapping("/{id}")
    public R<RedemptionEligibilityPolicyVo> getInfo(@PathVariable Long id) {
        return R.ok(redemptionEligibilityPolicyService.queryById(id));
    }

    @SaCheckPermission("redemption:eligibilityPolicy:add")
    @Log(title = "Redemption eligibility policy add", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody RedemptionEligibilityPolicyBo bo) {
        return toAjax(redemptionEligibilityPolicyService.insertByBo(bo));
    }

    @SaCheckPermission("redemption:eligibilityPolicy:edit")
    @Log(title = "Redemption eligibility policy edit", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RedemptionEligibilityPolicyBo bo) {
        return toAjax(redemptionEligibilityPolicyService.updateByBo(bo));
    }
}
