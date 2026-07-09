package com.gameluck.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.system.domain.SysConfig;

/**
 * 参数配置业务对象 sys_config
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysConfig.class, reverseConvertGenerate = false)
public class SysConfigBo extends BaseEntity {

    /**
     * 参数主键
     */
    private Long configId;

    /**
     * 参数名称
     */
    @NotBlank(message = "{system.config.name.required}")
    @Size(min = 0, max = 100, message = "{system.config.name.length}")
    private String configName;

    /**
     * 参数键名
     */
    @NotBlank(message = "{system.config.key.required}")
    @Size(min = 0, max = 100, message = "{system.config.key.length}")
    private String configKey;

    /**
     * 参数键值
     */
    @NotBlank(message = "{system.config.value.required}")
    @Size(min = 0, max = 500, message = "{system.config.value.length}")
    private String configValue;

    /**
     * 系统内置（Y是 N否）
     */
    private String configType;

    /**
     * 备注
     */
    private String remark;


}
