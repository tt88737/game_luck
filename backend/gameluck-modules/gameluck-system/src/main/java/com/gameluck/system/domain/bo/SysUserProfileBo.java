package com.gameluck.system.domain.bo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.gameluck.common.core.constant.RegexConstants;
import com.gameluck.common.core.xss.Xss;
import com.gameluck.common.mybatis.core.domain.BaseEntity;
import com.gameluck.common.sensitive.annotation.Sensitive;
import com.gameluck.common.sensitive.core.SensitiveStrategy;

/**
 * 个人信息业务处理
 *
 * @author Michelle.Chung
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SysUserProfileBo extends BaseEntity {

    /**
     * 用户昵称
     */
    @Xss(message = "{system.user.nick.name.xss}")
    @Size(min = 0, max = 30, message = "{system.user.nick.name.length}")
    private String nickName;

    /**
     * 用户邮箱
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    @Email(message = "{user.email.not.valid}")
    @Size(min = 0, max = 50, message = "{system.user.email.length}")
    private String email;

    /**
     * 手机号码
     */
    @Pattern(regexp = RegexConstants.MOBILE, message = "{user.mobile.phone.number.not.valid}")
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phonenumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;

}
