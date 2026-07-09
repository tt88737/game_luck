package com.gameluck.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.core.utils.SpringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页
 *
 * @author Lion Li
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
public class IndexController {

    /**
     * 访问首页，提示语
     */
    @GetMapping("/")
    public String index() {
        return MessageUtils.message("index.welcome", SpringUtils.getApplicationName());
    }

}
