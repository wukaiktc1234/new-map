package com.foodtraceability.cache;

public class ClientContextHolder {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClientContextHolder.class);
    private static final ThreadLocal<ClientType> CLIENT_TYPE = new ThreadLocal<>();

    public static void setClientType(ClientType clientType) {
        CLIENT_TYPE.set(clientType);
        log.debug("设置客户端类型: {}", clientType.getDescription());
    }

    public static ClientType getClientType() {
        ClientType clientType = CLIENT_TYPE.get();
        if (clientType == null) {
            clientType = ClientType.WEB;
            log.debug("未设置客户端类型，使用默认: {}", clientType.getDescription());
        }
        return clientType;
    }

    public static void clear() {
        CLIENT_TYPE.remove();
        log.debug("清除客户端类型上下文");
    }

    public static String getClientCode() {
        return getClientType().getCode();
    }
}
