package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.UserSession;
import com.foodtraceability.mapper.UserSessionMapper;
import com.foodtraceability.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话服务实现类
 *
 * 重构说明：
 * - 已移除 Redis 依赖（RedisTemplate）
 * - 会话数据由数据库直接管理，无需缓存层
 * - 保留所有数据库操作逻辑
 */
@Service
public class UserSessionServiceImpl implements UserSessionService {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionServiceImpl.class);


    public UserSessionServiceImpl(UserSessionMapper userSessionMapper) {
        this.userSessionMapper = userSessionMapper;
    }

    private final UserSessionMapper userSessionMapper;

    @Override
    public List<UserSession> getUserSessions(String userId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("status", 1)
                   .orderByDesc("login_time");

        List<UserSession> sessions = userSessionMapper.selectList(queryWrapper);

        logger.info("Retrieved {} active sessions for userId={}", sessions.size(), userId);
        return sessions;
    }

    @Override
    public UserSession createSession(UserSession session) {
        session.setLoginTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        session.setStatus(1);
        session.setCreatedAt(LocalDateTime.now());

        userSessionMapper.insert(session);

        logger.info("Session created: sessionId={}, userId={}, deviceId={}",
            session.getId(), session.getUserId(), session.getDeviceId());

        return session;
    }

    @Override
    public void updateLastActiveTime(String sessionId) {
        UserSession session = userSessionMapper.selectById(Long.parseLong(sessionId));
        if (session != null) {
            session.setLastActiveTime(LocalDateTime.now());
            userSessionMapper.updateById(session);
        }
    }

    @Override
    public void logoutSession(String sessionId) {
        UserSession session = userSessionMapper.selectById(Long.parseLong(sessionId));
        if (session != null) {
            session.setStatus(0);
            userSessionMapper.updateById(session);

            logger.info("Session logged out: sessionId={}, userId={}",
                session.getId(), session.getUserId());
        }
    }

    @Override
    public void logoutAllSessions(String userId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("status", 1);

        List<UserSession> sessions = userSessionMapper.selectList(queryWrapper);

        for (UserSession session : sessions) {
            session.setStatus(0);
            userSessionMapper.updateById(session);
        }

        logger.info("All sessions logged out: userId={}, sessionCount={}",
            userId, sessions.size());
    }

    @Override
    public void logoutOtherSessions(String userId, String currentSessionId) {
        QueryWrapper<UserSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("status", 1)
                   .ne("id", currentSessionId);

        List<UserSession> sessions = userSessionMapper.selectList(queryWrapper);

        for (UserSession session : sessions) {
            session.setStatus(0);
            userSessionMapper.updateById(session);
        }

        logger.info("Other sessions logged out: userId={}, currentSessionId={}, loggedOutCount={}",
            userId, currentSessionId, sessions.size());
    }

    @Override
    public void deleteSession(String sessionId) {
        userSessionMapper.deleteById(Long.parseLong(sessionId));

        logger.info("Session deleted: sessionId={}", sessionId);
    }
}
