package com.gameluck.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.system.domain.SysPost;

/**
 * 岗位信息业务对象 sys_post
 *
 * @author Michelle.Chung
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysPost.class, reverseConvertGenerate = false)
public class SysPostBo extends BaseEntity {

    /**
     * 岗位ID
     */
    private Long postId;

    /**
     * 部门id（单部门）
     */
    @NotNull(message = "{system.dept.id.required}")
    private Long deptId;

    /**
     * 归属部门id（部门树）
     */
    private Long belongDeptId;

    /**
     * 岗位编码
     */
    @NotBlank(message = "{system.post.code.required}")
    @Size(min = 0, max = 64, message = "{system.post.code.length}")
    private String postCode;

    /**
     * 岗位名称
     */
    @NotBlank(message = "{system.post.name.required}")
    @Size(min = 0, max = 50, message = "{system.post.name.length}")
    private String postName;

    /**
     * 岗位类别编码
     */
    @Size(min = 0, max = 100, message = "{system.post.category.length}")
    private String postCategory;

    /**
     * 显示顺序
     */
    @NotNull(message = "{system.display.sort.required}")
    private Integer postSort;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
