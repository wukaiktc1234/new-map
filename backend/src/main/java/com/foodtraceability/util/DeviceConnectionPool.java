package com.foodtraceability.util;

import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 设备连接池管理类
 * 用于管理和复用设备连接，减少连接创建和销毁的开销
 */
public class DeviceConnectionPool {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceConnectionPool.class);
    
    // 连接池最大连接数
    private static final int DEFAULT_MAX_CONNECTIONS = 10;
    // 连接池最小空闲连接数
    private static final int DEFAULT_MIN_IDLE_CONNECTIONS = 2;
    // 连接超时时间（毫秒）
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    // 连接最大空闲时间（毫秒）
    private static final long DEFAULT_MAX_IDLE_TIME = 30 * 60 * 1000;
    // 连接验证超时时间（毫秒）
    private static final int DEFAULT_VALIDATE_TIMEOUT = 1000;
    
    // 自动重连最大尝试次数
    private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 3;
    
    // 自动重连间隔时间（毫秒）
    private static final long DEFAULT_RECONNECT_INTERVAL = 5000;
    
    // 设备配置
    private final HardwareConfig config;
    // 连接池队列
    private final BlockingQueue<DeviceConnection> connectionQueue;
    // 连接池最大连接数
    private final int maxConnections;
    // 连接池最小空闲连接数
    private final int minIdleConnections;
    // 连接超时时间（毫秒）
    private final int connectTimeout;
    // 连接最大空闲时间（毫秒）
    private final long maxIdleTime;
    // 连接验证超时时间（毫秒）
    private final int validateTimeout;
    // 自动重连最大尝试次数
    private final int maxReconnectAttempts;
    // 自动重连间隔时间（毫秒）
    private final long reconnectInterval;
    // 当前活跃连接数
    private volatile int activeConnections;
    // 连接池是否已关闭
    private volatile boolean closed;
    
    /**
     * 设备连接包装类
     */
    public static class DeviceConnection {
        // 原始Socket连接
        private final Socket socket;
        // 连接创建时间
        private final long createTime;
        // 最后使用时间
        private volatile long lastUsedTime;
        // 是否在使用中
        private volatile boolean inUse;
        
        public DeviceConnection(Socket socket) {
            this.socket = socket;
            this.createTime = System.currentTimeMillis();
            this.lastUsedTime = System.currentTimeMillis();
            this.inUse = false;
        }
        
        public Socket getSocket() {
            return socket;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public long getLastUsedTime() {
            return lastUsedTime;
        }
        
        public void updateLastUsedTime() {
            this.lastUsedTime = System.currentTimeMillis();
        }
        
        public boolean isInUse() {
            return inUse;
        }
        
        public void setInUse(boolean inUse) {
            this.inUse = inUse;
        }
        
        /**
         * 检查连接是否过期
         * @param maxIdleTime 最大空闲时间（毫秒）
         * @return true表示连接已过期
         */
        public boolean isExpired(long maxIdleTime) {
            return System.currentTimeMillis() - lastUsedTime > maxIdleTime;
        }
        
        /**
         * 检查连接是否有效
         * @return true表示连接有效
         */
        public boolean isValid() {
            return socket != null && !socket.isClosed() && socket.isConnected();
        }
        
        /**
         * 关闭连接
         */
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.warn("关闭设备连接失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 构造函数
     * @param config 设备配置
     */
    public DeviceConnectionPool(HardwareConfig config) {
        this(config, DEFAULT_MAX_CONNECTIONS, DEFAULT_MIN_IDLE_CONNECTIONS);
    }
    
    /**
     * 构造函数
     * @param config 设备配置
     * @param maxConnections 最大连接数
     * @param minIdleConnections 最小空闲连接数
     */
    public DeviceConnectionPool(HardwareConfig config, int maxConnections, int minIdleConnections) {
        this(config, maxConnections, minIdleConnections, DEFAULT_CONNECT_TIMEOUT, DEFAULT_MAX_IDLE_TIME, DEFAULT_VALIDATE_TIMEOUT);
    }
    
    /**
     * 构造函数
     * @param config 设备配置
     * @param maxConnections 最大连接数
     * @param minIdleConnections 最小空闲连接数
     * @param connectTimeout 连接超时时间（毫秒）
     * @param maxIdleTime 连接最大空闲时间（毫秒）
     * @param validateTimeout 连接验证超时时间（毫秒）
     */
    public DeviceConnectionPool(HardwareConfig config, int maxConnections, int minIdleConnections, 
                               int connectTimeout, long maxIdleTime, int validateTimeout) {
        this(config, maxConnections, minIdleConnections, connectTimeout, maxIdleTime, validateTimeout, 
             DEFAULT_MAX_RECONNECT_ATTEMPTS, DEFAULT_RECONNECT_INTERVAL);
    }
    
    /**
     * 构造函数
     * @param config 设备配置
     * @param maxConnections 最大连接数
     * @param minIdleConnections 最小空闲连接数
     * @param connectTimeout 连接超时时间（毫秒）
     * @param maxIdleTime 连接最大空闲时间（毫秒）
     * @param validateTimeout 连接验证超时时间（毫秒）
     * @param maxReconnectAttempts 自动重连最大尝试次数
     * @param reconnectInterval 自动重连间隔时间（毫秒）
     */
    public DeviceConnectionPool(HardwareConfig config, int maxConnections, int minIdleConnections, 
                               int connectTimeout, long maxIdleTime, int validateTimeout, 
                               int maxReconnectAttempts, long reconnectInterval) {
        this.config = config;
        this.maxConnections = maxConnections;
        this.minIdleConnections = minIdleConnections;
        this.connectTimeout = connectTimeout;
        this.maxIdleTime = maxIdleTime;
        this.validateTimeout = validateTimeout;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.reconnectInterval = reconnectInterval;
        this.connectionQueue = new LinkedBlockingQueue<>(maxConnections);
        this.activeConnections = 0;
        this.closed = false;
        
        // 初始化连接池，创建最小空闲连接数
        init();
    }
    
    /**
     * 初始化连接池
     */
    private void init() {
        try {
            for (int i = 0; i < minIdleConnections; i++) {
                DeviceConnection connection = createConnection();
                if (connection != null) {
                    connectionQueue.offer(connection);
                }
            }
        } catch (Exception e) {
            log.error("初始化设备连接池失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建新连接
     * @return 设备连接
     */
    private DeviceConnection createConnection() {
        try {
            if (activeConnections >= maxConnections) {
                return null;
            }
            
            String ip = config.getIpAddress();
            int port = Integer.parseInt(config.getPort());
            
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), connectTimeout);
            socket.setSoTimeout(connectTimeout);
            
            DeviceConnection connection = new DeviceConnection(socket);
            activeConnections++;
            log.info("创建新设备连接: {}:{}, 连接数: {}/{} (活跃: {}, 空闲: {})", 
                    ip, port, activeConnections, maxConnections, activeConnections - connectionQueue.size(), connectionQueue.size());
            
            return connection;
        } catch (Exception e) {
            log.error("创建设备连接失败: {}:{}, 错误: {}", 
                    config.getIpAddress(), config.getPort(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 从连接池获取连接
     * @return 设备连接
     */
    public DeviceConnection getConnection() {
        return getConnection(connectTimeout);
    }
    
    /**
     * 从连接池获取连接
     * @param timeout 超时时间（毫秒）
     * @return 设备连接
     */
    public DeviceConnection getConnection(long timeout) {
        if (closed) {
            throw new IllegalStateException("连接池已关闭");
        }
        
        long startTime = System.currentTimeMillis();
        int reconnectAttempts = 0;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            // 尝试从队列中获取连接
            DeviceConnection connection = connectionQueue.poll();
            if (connection != null) {
                // 检查连接是否有效
                if (isValidConnection(connection)) {
                    connection.setInUse(true);
                    connection.updateLastUsedTime();
                    log.debug("从连接池获取连接: {}:{}, 连接数: {}/{} (活跃: {}, 空闲: {})", 
                            config.getIpAddress(), config.getPort(), activeConnections, maxConnections, 
                            activeConnections - connectionQueue.size(), connectionQueue.size());
                    return connection;
                } else {
                    // 连接无效，关闭并减少活跃连接数
                    connection.close();
                    activeConnections--;
                    log.info("移除无效设备连接: {}:{}, 连接数: {}/{} (活跃: {}, 空闲: {})", 
                            config.getIpAddress(), config.getPort(), activeConnections, maxConnections, 
                            activeConnections - connectionQueue.size(), connectionQueue.size());
                }
            } else if (activeConnections < maxConnections) {
                // 队列中没有可用连接，且未达到最大连接数，创建新连接
                DeviceConnection newConnection = createConnectionWithReconnect(reconnectAttempts);
                if (newConnection != null) {
                    newConnection.setInUse(true);
                    newConnection.updateLastUsedTime();
                    return newConnection;
                } else {
                    // 创建连接失败，尝试自动重连
                    reconnectAttempts++;
                    if (reconnectAttempts >= maxReconnectAttempts) {
                        log.error("设备连接失败，已达到最大重连次数: {}:{}, 重连次数: {}/{}", 
                                config.getIpAddress(), config.getPort(), reconnectAttempts, maxReconnectAttempts);
                        break;
                    }
                    
                    // 等待重连间隔时间后重试
                    try {
                        log.info("设备连接失败，将在 {}ms 后尝试重连: {}:{}, 重连次数: {}/{}", 
                                reconnectInterval, config.getIpAddress(), config.getPort(), reconnectAttempts, maxReconnectAttempts);
                        Thread.sleep(reconnectInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // 等待一段时间后重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return null;
    }
    
    /**
     * 创建连接并支持自动重连
     * @param attempt 当前尝试次数
     * @return 设备连接
     */
    private DeviceConnection createConnectionWithReconnect(int attempt) {
        try {
            return createConnection();
        } catch (Exception e) {
            log.warn("创建设备连接失败，尝试次数: {}/{}", attempt + 1, maxReconnectAttempts, e);
            return null;
        }
    }
    
    /**
     * 检查连接是否有效
     * @param connection 设备连接
     * @return true表示连接有效
     */
    private boolean isValidConnection(DeviceConnection connection) {
        if (connection == null || !connection.isValid() || connection.isExpired(maxIdleTime)) {
            return false;
        }
        
        // 发送心跳包验证连接
        try {
            Socket socket = connection.getSocket();
            OutputStream outputStream = socket.getOutputStream();
            // 发送一个简单的心跳命令（ESC @ 初始化打印机）
            byte[] heartbeat = { 0x1B, 0x40 };
            outputStream.write(heartbeat);
            outputStream.flush();
            return true;
        } catch (Exception e) {
            log.warn("验证设备连接无效: {}:{}, 错误: {}", 
                    config.getIpAddress(), config.getPort(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 归还连接到连接池
     * @param connection 设备连接
     */
    public void returnConnection(DeviceConnection connection) {
        if (closed) {
            connection.close();
            activeConnections--;
            return;
        }
        
        if (connection == null) {
            return;
        }
        
        try {
            if (!connection.isValid() || connection.isExpired(maxIdleTime)) {
                // 连接无效，关闭并减少活跃连接数
                connection.close();
                activeConnections--;
                log.info("关闭过期设备连接: {}:{}, 连接数: {}/{} (活跃: {}, 空闲: {})", 
                        config.getIpAddress(), config.getPort(), activeConnections, maxConnections, 
                        activeConnections - connectionQueue.size(), connectionQueue.size());
                return;
            }
            
            // 重置连接状态
            connection.setInUse(false);
            connection.updateLastUsedTime();
            
            // 将连接放回队列
            if (!connectionQueue.offer(connection)) {
                // 队列已满，关闭连接
                connection.close();
                activeConnections--;
                log.info("连接池队列已满，关闭设备连接: {}:{}, 连接数: {}/{} (活跃: {}, 空闲: {})", 
                        config.getIpAddress(), config.getPort(), activeConnections, maxConnections, 
                        activeConnections - connectionQueue.size(), connectionQueue.size());
            } else {
                log.debug("归还设备连接到连接池: {}:{}, 连接数: {}/{} (活跃: {}, 空闲: {})", 
                        config.getIpAddress(), config.getPort(), activeConnections, maxConnections, 
                        activeConnections - connectionQueue.size(), connectionQueue.size());
            }
        } catch (Exception e) {
            log.error("归还设备连接失败: {}:{}, 错误: {}", 
                    config.getIpAddress(), config.getPort(), e.getMessage());
            // 归还失败，关闭连接
            connection.close();
            activeConnections--;
        }
    }
    
    /**
     * 关闭连接池
     */
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        
        // 关闭所有连接
        List<DeviceConnection> connections = new ArrayList<>();
        connectionQueue.drainTo(connections);
        for (DeviceConnection connection : connections) {
            connection.close();
        }
        
        activeConnections = 0;
        log.info("关闭设备连接池: {}:{}, 释放连接数: {}", 
                config.getIpAddress(), config.getPort(), connections.size());
    }
    
    /**
     * 获取连接池状态
     * @return 连接池状态
     */
    public ConnectionPoolStatus getStatus() {
        return new ConnectionPoolStatus(
                activeConnections,
                maxConnections,
                connectionQueue.size(),
                activeConnections - connectionQueue.size(),
                closed
        );
    }
    
    /**
     * 连接池状态类
     */
    public static class ConnectionPoolStatus {
        private final int activeConnections;
        private final int maxConnections;
        private final int idleConnections;
        private final int inUseConnections;
        private final boolean closed;
        
        public ConnectionPoolStatus(int activeConnections, int maxConnections, 
                                  int idleConnections, int inUseConnections, boolean closed) {
            this.activeConnections = activeConnections;
            this.maxConnections = maxConnections;
            this.idleConnections = idleConnections;
            this.inUseConnections = inUseConnections;
            this.closed = closed;
        }
        
        public int getActiveConnections() {
            return activeConnections;
        }
        
        public int getMaxConnections() {
            return maxConnections;
        }
        
        public int getIdleConnections() {
            return idleConnections;
        }
        
        public int getInUseConnections() {
            return inUseConnections;
        }
        
        public boolean isClosed() {
            return closed;
        }
        
        @Override
        public String toString() {
            return String.format("ConnectionPoolStatus{active=%d, max=%d, idle=%d, inUse=%d, closed=%b}",
                    activeConnections, maxConnections, idleConnections, inUseConnections, closed);
        }
    }
}