package com.foodtraceability.service;

import com.foodtraceability.dto.SessionDeviceResponse;
import java.util.List;

public interface SessionService {

    void registerSession(String userId, String token, String deviceInfo, String ipAddress);

    void removeSession(String userId, String tokenId);

    /**
     * 根据JWT令牌移除对应的会话记录
     * 用于登出时清理会话（logout只有token，没有tokenId）
     * @param userId 用户ID
     * @param token JWT令牌
     */
    void removeSessionByToken(String userId, String token);

    void removeAllOtherSessions(String userId, String currentTokenId);
    
    List<SessionDeviceResponse> getUserSessions(String userId);
    
    boolean isSessionValid(String userId, String tokenId);
    
    void cleanupExpiredSessions(String userId);
}
