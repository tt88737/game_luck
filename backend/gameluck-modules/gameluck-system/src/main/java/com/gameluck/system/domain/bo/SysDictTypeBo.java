package com.gameluck.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.gameluck.common.core.constant.RegexConstants;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.system.domain.SysDictType;

/**
 * 字典类型业务对象 sys_dict_type
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDictType.class, reverseConvertGenerate = false)
public class SysDictTypeBo extends BaseEntity {

    /**
     * 字典主键
     */
    private Long dictId;

    /**
     * 字典名称
     */
    @NotBlank(message = "{system.dict.name.required}")
    @Size(min = 0, max = 100, message = "{system.dict.name.length}")
    private String dictName;

    /**
     * 字典类型
     */
    @NotBlank(message = "{system.dict.type.required}")
    @Size(min = 0, max = 100, message = "{system.dict.type.length}")
    @Pattern(regexp = RegexConstants.DICTIONARY_TYPE, message = "{system.dict.type.format}")
    private String dictType;

    /**
     * 备注
     */
    private String remark;


}
