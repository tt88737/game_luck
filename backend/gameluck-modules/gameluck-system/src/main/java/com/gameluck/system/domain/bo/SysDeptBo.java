package com.gameluck.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.system.domain.SysDept;

/**
 * 部门业务对象 sys_dept
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDept.class, reverseConvertGenerate = false)
public class SysDeptBo extends BaseEntity {

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 部门名称
     */
    @NotBlank(message = "{system.dept.name.required}")
    @Size(min = 0, max = 30, message = "{system.dept.name.length}")
    private String deptName;

    /**
     * 部门类别编码
     */
    @Size(min = 0, max = 100, message = "{system.dept.category.length}")
    private String deptCategory;

    /**
     * 显示顺序
     */
    @NotNull(message = "{system.display.sort.required}")
    private Integer orderNum;

    /**
     * 负责人
     */
    private Long leader;

    /**
     * 联系电话
     */
    @Size(min = 0, max = 11, message = "{system.dept.phone.length}")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "{user.email.not.valid}")
    @Size(min = 0, max = 50, message = "{system.dept.email.length}")
    private String email;

    /**
     * 部门状态（0正常 1停用）
     */
    private String status;

    /**
     * 归属部门id（部门树）
     */
    private Long belongDeptId;

}
