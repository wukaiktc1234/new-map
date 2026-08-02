package com.foodtraceability.util;

import java.util.Random;

/**
 * 设备重连策略接口
 * 定义设备重连策略的统一接口
 */
public interface DeviceReconnectStrategy {
    
    /**
     * 获取下一次重连的间隔时间（毫秒）
     * @param retryCount 已重试次数
     * @return 下一次重连的间隔时间
     */
    long getNextReconnectInterval(int retryCount);
    
    /**
     * 是否应该继续重连
     * @param retryCount 已重试次数
     * @return 是否应该继续重连
     */
    boolean shouldContinueReconnecting(int retryCount);
    
    /**
     * 重置重连策略
     */
    void reset();
    
    /**
     * 固定间隔重连策略
     */
    class FixedIntervalReconnectStrategy implements DeviceReconnectStrategy {
        
        // 默认重连间隔（毫秒）
        private static final long DEFAULT_INTERVAL = 5000;
        
        // 最大重试次数
        private static final int DEFAULT_MAX_RETRIES = 5;
        
        private final long interval;
        private final int maxRetries;
        
        public FixedIntervalReconnectStrategy() {
            this(DEFAULT_INTERVAL, DEFAULT_MAX_RETRIES);
        }
        
        public FixedIntervalReconnectStrategy(long interval, int maxRetries) {
            this.interval = interval;
            this.maxRetries = maxRetries;
        }
        
        @Override
        public long getNextReconnectInterval(int retryCount) {
            return interval;
        }
        
        @Override
        public boolean shouldContinueReconnecting(int retryCount) {
            return retryCount < maxRetries;
        }
        
        @Override
        public void reset() {
            // 固定间隔策略无需重置
        }
    }
    
    /**
     * 指数退避重连策略
     */
    class ExponentialBackoffReconnectStrategy implements DeviceReconnectStrategy {
        
        // 默认初始重连间隔（毫秒）
        private static final long DEFAULT_INITIAL_INTERVAL = 1000;
        
        // 默认最大重连间隔（毫秒）
        private static final long DEFAULT_MAX_INTERVAL = 60000;
        
        // 默认最大重试次数
        private static final int DEFAULT_MAX_RETRIES = 10;
        
        // 退避因子
        private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
        
        private final long initialInterval;
        private final long maxInterval;
        private final int maxRetries;
        private final double backoffMultiplier;
        private final Random random;
        
        public ExponentialBackoffReconnectStrategy() {
            this(DEFAULT_INITIAL_INTERVAL, DEFAULT_MAX_INTERVAL, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULTIPLIER);
        }
        
        public ExponentialBackoffReconnectStrategy(long initialInterval, long maxInterval, int maxRetries, double backoffMultiplier) {
            this.initialInterval = initialInterval;
            this.maxInterval = maxInterval;
            this.maxRetries = maxRetries;
            this.backoffMultiplier = backoffMultiplier;
            this.random = new Random();
        }
        
        @Override
        public long getNextReconnectInterval(int retryCount) {
            // 计算指数退避间隔，并添加一定的随机性
            long interval = (long) (initialInterval * Math.pow(backoffMultiplier, retryCount));
            // 添加随机抖动，避免所有设备同时重连
            interval = (long) (interval * (0.8 + random.nextDouble() * 0.4));
            // 限制最大间隔
            return Math.min(interval, maxInterval);
        }
        
        @Override
        public boolean shouldContinueReconnecting(int retryCount) {
            return retryCount < maxRetries;
        }
        
        @Override
        public void reset() {
            // 指数退避策略无需重置
        }
    }
    
    /**
     * 随机间隔重连策略
     */
    class RandomIntervalReconnectStrategy implements DeviceReconnectStrategy {
        
        // 默认最小重连间隔（毫秒）
        private static final long DEFAULT_MIN_INTERVAL = 1000;
        
        // 默认最大重连间隔（毫秒）
        private static final long DEFAULT_MAX_INTERVAL = 10000;
        
        // 默认最大重试次数
        private static final int DEFAULT_MAX_RETRIES = 8;
        
        private final long minInterval;
        private final long maxInterval;
        private final int maxRetries;
        private final Random random;
        
        public RandomIntervalReconnectStrategy() {
            this(DEFAULT_MIN_INTERVAL, DEFAULT_MAX_INTERVAL, DEFAULT_MAX_RETRIES);
        }
        
        public RandomIntervalReconnectStrategy(long minInterval, long maxInterval, int maxRetries) {
            this.minInterval = minInterval;
            this.maxInterval = maxInterval;
            this.maxRetries = maxRetries;
            this.random = new Random();
        }
        
        @Override
        public long getNextReconnectInterval(int retryCount) {
            return minInterval + random.nextLong(maxInterval - minInterval + 1);
        }
        
        @Override
        public boolean shouldContinueReconnecting(int retryCount) {
            return retryCount < maxRetries;
        }
        
        @Override
        public void reset() {
            // 随机间隔策略无需重置
        }
    }
    
    /**
     * 重连策略工厂类
     */
    class ReconnectStrategyFactory {
        
        /**
         * 获取默认的重连策略
         * @return 默认的重连策略
         */
        public static DeviceReconnectStrategy getDefaultStrategy() {
            return new ExponentialBackoffReconnectStrategy();
        }
        
        /**
         * 获取固定间隔重连策略
         * @param interval 重连间隔（毫秒）
         * @param maxRetries 最大重试次数
         * @return 固定间隔重连策略
         */
        public static DeviceReconnectStrategy getFixedIntervalStrategy(long interval, int maxRetries) {
            return new FixedIntervalReconnectStrategy(interval, maxRetries);
        }
        
        /**
         * 获取指数退避重连策略
         * @param initialInterval 初始重连间隔（毫秒）
         * @param maxInterval 最大重连间隔（毫秒）
         * @param maxRetries 最大重试次数
         * @param backoffMultiplier 退避因子
         * @return 指数退避重连策略
         */
        public static DeviceReconnectStrategy getExponentialBackoffStrategy(long initialInterval, long maxInterval, int maxRetries, double backoffMultiplier) {
            return new ExponentialBackoffReconnectStrategy(initialInterval, maxInterval, maxRetries, backoffMultiplier);
        }
        
        /**
         * 获取随机间隔重连策略
         * @param minInterval 最小重连间隔（毫秒）
         * @param maxInterval 最大重连间隔（毫秒）
         * @param maxRetries 最大重试次数
         * @return 随机间隔重连策略
         */
        public static DeviceReconnectStrategy getRandomIntervalStrategy(long minInterval, long maxInterval, int maxRetries) {
            return new RandomIntervalReconnectStrategy(minInterval, maxInterval, maxRetries);
        }
    }
}