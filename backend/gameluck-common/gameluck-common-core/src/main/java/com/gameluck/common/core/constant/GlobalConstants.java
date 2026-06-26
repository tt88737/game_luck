package com.gameluck.common.core.constant;

/**
 * Global redis key constants.
 */
public interface GlobalConstants {

    /**
     * Global redis key prefix.
     */
    String GLOBAL_REDIS_KEY = "global:";

    /**
     * Captcha redis key.
     */
    String CAPTCHA_CODE_KEY = GLOBAL_REDIS_KEY + "captcha_codes:";

    /**
     * Repeat-submit redis key.
     */
    String REPEAT_SUBMIT_KEY = GLOBAL_REDIS_KEY + "repeat_submit:";

    /**
     * Rate-limit redis key.
     */
    String RATE_LIMIT_KEY = GLOBAL_REDIS_KEY + "rate_limit:";
}
