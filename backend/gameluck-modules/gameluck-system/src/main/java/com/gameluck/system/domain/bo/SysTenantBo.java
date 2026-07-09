package com.gameluck.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.gameluck.common.core.validate.AddGroup;
import com.gameluck.common.core.validate.EditGroup;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.system.domain.SysTenant;

import java.util.Date;

/**
 * 租户业务对象 sys_tenant
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTenant.class, reverseConvertGenerate = false)
public class SysTenantBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "{common.primary.key.required}", groups = { EditGroup.class })
    private Long id;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 联系人
     */
    @NotBlank(message = "{system.tenant.contact.required}", groups = { AddGroup.class, EditGroup.class })
    private String contactUserName;

    /**
     * 联系电话
     */
    @NotBlank(message = "{system.tenant.contact.phone.required}", groups = { AddGroup.class, EditGroup.class })
    private String contactPhone;

    /**
     * 企业名称
     */
    @NotBlank(message = "{system.tenant.company.required}", groups = { AddGroup.class, EditGroup.class })
    private String companyName;

    /**
     * 用户名（创建系统用户）
     */
    @NotBlank(message = "{system.tenant.username.required}", groups = { AddGroup.class })
    private String username;

    /**
     * 密码（创建系统用户）
     */
    @NotBlank(message = "{system.tenant.password.required}", groups = { AddGroup.class })
//    @Pattern(regexp = RegexConstants.PASSWORD, message = "{user.password.format.valid}", groups = { AddGroup.class })
    private String password;

    /**
     * 统一社会信用代码
     */
    private String licenseNumber;

    /**
     * 地址
     */
    private String address;

    /**
     * 域名
     */
    private String domain;

    /**
     * 企业简介
     */
    private String intro;

    /**
     * 备注
     */
    private String remark;

    /**
     * 租户套餐编号
     */
    @NotNull(message = "{system.tenant.package.required}", groups = { AddGroup.class })
    private Long packageId;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户数量（-1不限制）
     */
    private Long accountCount;

    /**
     * 租户状态（0正常 1停用）
     */
    private String status;


}
