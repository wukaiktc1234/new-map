package com.foodtraceability.cache;

public enum ClientType {
    MINIPROGRAM("miniprogram", "小程序"),
    DELIVERY_PLATFORM("delivery", "外卖平台"),
    DESKTOP_QRCODE("desktop", "桌面二维码"),
    APP("app", "APP"),
    CASHIER("cashier", "前端收银"),
    WEB("web", "Web端"),
    ADMIN("admin", "管理后台");

    private final String code;
    private final String description;

    ClientType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ClientType fromCode(String code) {
        if (code == null) {
            return WEB;
        }
        for (ClientType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return WEB;
    }
}
