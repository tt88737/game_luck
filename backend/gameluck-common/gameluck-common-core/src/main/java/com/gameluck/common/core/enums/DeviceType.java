package com.gameluck.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备类型
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum DeviceType {

    /**
     * pc端
     */
    PC("pc"),

    /**
     * app绔?
     */
    APP("app"),

    /**
     * 灏忕▼搴忕
     */
    XCX("xcx");

    /**
     * 设备标识
     */
    private final String device;
}
