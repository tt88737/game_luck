package com.gameluck.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.gameluck.common.core.constant.SystemConstants;
import com.gameluck.common.core.domain.R;
import com.gameluck.common.core.domain.model.LoginBody;
import com.gameluck.common.core.domain.model.RegisterBody;
import com.gameluck.common.core.utils.DateUtils;
import com.gameluck.common.core.utils.MapstructUtils;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.StreamUtils;
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.common.core.utils.ValidatorUtils;
import com.gameluck.common.encrypt.annotation.ApiEncrypt;
import com.gameluck.common.json.utils.JsonUtils;
import com.gameluck.common.ratelimiter.annotation.RateLimiter;
import com.gameluck.common.ratelimiter.enums.LimitType;
import com.gameluck.common.satoken.utils.LoginHelper;
import com.gameluck.common.sse.dto.SseMessageDto;
import com.gameluck.common.sse.utils.SseMessageUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.system.domain.bo.SysTenantBo;
import com.gameluck.system.domain.vo.SysClientVo;
import com.gameluck.system.domain.vo.SysTenantVo;
import com.gameluck.system.service.ISysClientService;
import com.gameluck.system.service.ISysConfigService;
import com.gameluck.system.service.ISysTenantService;
import com.gameluck.web.domain.vo.LoginTenantVo;
import com.gameluck.web.domain.vo.LoginVo;
import com.gameluck.web.domain.vo.TenantListVo;
import com.gameluck.web.service.IAuthStrategy;
import com.gameluck.web.service.SysLoginService;
import com.gameluck.web.service.SysRegisterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysConfigService configService;
    private final ISysTenantService tenantService;
    private final ISysClientService clientService;
    private final ScheduledExecutorService scheduledExecutorService;

    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);

        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClientVo client = clientService.queryByClientId(clientId);
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), grantType)) {
            log.info("clientId: {} grantType: {} invalid.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }

        loginService.checkTenant(loginBody.getTenantId());
        LoginVo loginVo = IAuthStrategy.login(body, client, grantType);

        Long userId = LoginHelper.getUserId();
        scheduledExecutorService.schedule(() -> {
            SseMessageDto dto = new SseMessageDto();
            dto.setMessage(DateUtils.getTodayHour(new Date()) + ", welcome to GameLuck Admin");
            dto.setUserIds(List.of(userId));
            SseMessageUtils.publishMessage(dto);
        }, 5, TimeUnit.SECONDS);
        return R.ok(loginVo);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("logout success");
    }

    @ApiEncrypt
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user) {
        if (!configService.selectRegisterEnabled(user.getTenantId())) {
            return R.fail("registration is disabled");
        }
        registerService.register(user);
        return R.ok();
    }

    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @GetMapping("/tenant/list")
    public R<LoginTenantVo> tenantList(HttpServletRequest request) throws Exception {
        LoginTenantVo result = new LoginTenantVo();
        boolean enable = TenantHelper.isEnable();
        result.setTenantEnabled(enable);
        if (!enable) {
            return R.ok(result);
        }

        List<SysTenantVo> tenantList = tenantService.queryList(new SysTenantBo());
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        try {
            if (LoginHelper.isSuperAdmin()) {
                result.setVoList(voList);
                return R.ok(result);
            }
        } catch (NotLoginException ignored) {
        }

        String host;
        String referer = request.getHeader("referer");
        if (StringUtils.isNotBlank(referer)) {
            host = referer.split("//")[1].split("/")[0];
        } else {
            host = new URL(request.getRequestURL().toString()).getHost();
        }
        List<TenantListVo> list = StreamUtils.filter(voList, vo -> StringUtils.equalsIgnoreCase(vo.getDomain(), host));
        result.setVoList(CollUtil.isNotEmpty(list) ? list : voList);
        return R.ok(result);
    }
}
