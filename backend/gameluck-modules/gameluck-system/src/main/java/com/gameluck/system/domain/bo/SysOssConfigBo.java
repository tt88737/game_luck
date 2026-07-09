package com.gameluck.system.domain.bo;

import com.gameluck.common.core.validate.AddGroup;
import com.gameluck.common.core.validate.EditGroup;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.system.domain.SysOssConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对象存储配置业务对象 sys_oss_config
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysOssConfig.class, reverseConvertGenerate = false)
public class SysOssConfigBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "{common.primary.key.required}", groups = {EditGroup.class})
    private Long ossConfigId;

    /**
     * 配置key
     */
    @NotBlank(message = "{system.oss.config.key.required}", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "{system.oss.config.key.length}")
    private String configKey;

    /**
     * accessKey
     */
    @NotBlank(message = "{system.oss.access.key.required}", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "{system.oss.access.key.length}")
    private String accessKey;

    /**
     * 秘钥
     */
    @NotBlank(message = "{system.oss.secret.key.required}", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "{system.oss.secret.key.length}")
    private String secretKey;

    /**
     * 桶名称
     */
    @NotBlank(message = "{system.oss.bucket.name.required}", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "{system.oss.bucket.name.length}")
    private String bucketName;

    /**
     * 前缀
     */
    private String prefix;

    /**
     * 访问站点
     */
    @NotBlank(message = "{system.oss.endpoint.required}", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "{system.oss.endpoint.length}")
    private String endpoint;

    /**
     * 自定义域名
     */
    private String domain;

    /**
     * 是否https（Y=是,N=否）
     */
    private String isHttps;

    /**
     * 是否默认（0=是,1=否）
     */
    private String status;

    /**
     * 域
     */
    private String region;

    /**
     * 扩展字段
     */
    private String ext1;

    /**
     * 备注
     */
    private String remark;

    /**
     * 桶权限类型(0private 1public 2custom)
     */
    @NotBlank(message = "{system.oss.access.policy.required}", groups = {AddGroup.class, EditGroup.class})
    private String accessPolicy;

}
