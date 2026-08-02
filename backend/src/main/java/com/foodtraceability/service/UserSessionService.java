package com.foodtraceability.service;

import com.foodtraceability.entity.UserSession;

import java.util.List;

public interface UserSessionService {
    
    List<UserSession> getUserSessions(String userId);
    
    UserSession createSession(UserSession session);
    
    void updateLastActiveTime(String sessionId);
    
    void logoutSession(String sessionId);
    
    void logoutAllSessions(String userId);
    
    void logoutOtherSessions(String userId, String currentSessionId);
    
    void deleteSession(String sessionId);
}