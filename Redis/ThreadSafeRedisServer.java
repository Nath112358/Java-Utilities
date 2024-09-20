package com.refocus;

import java.util.concurrent.atomic.AtomicBoolean;

import com.refocus.executor.gateway.redis.RedisGateway;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.embedded.RedisServer;
import java.util.Objects;

@Slf4j
public class ThreadSafeRedisServer {
    private static RedisServer redisServer;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public synchronized static void start() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                if (redisServer == null) {
                    log.info("Building new Redis Server...");
                    redisServer = RedisServer.builder().port(6370).build();
                }
                log.info("Starting new Redis Server...");
                redisServer.start();
            } catch (Exception e) {
                isRunning.set(false);
                log.error("Failed to start Redis Server -> \n {}", e.getMessage());
                throw e;
            }
        } else log.info("Redis Server is already running");
    }

    public synchronized static void checkConnectionToRedisServer(RedisGateway redisGateway) throws InterruptedException {
        int maxRetries = 10;
        int retryIntervalMs = 1000;
        int i = 0;
        try {
            for (i = 0; i < maxRetries; i++) {
                try {
                    Objects.requireNonNull(redisGateway).getJedisPool().getResource().ping();
                    log.info("Successfully connected to Redis");
                    return;
                } catch (JedisConnectionException e) {
                    log.info("Failed to connect to Redis, retrying in {} ms ", retryIntervalMs);
                    Thread.sleep(retryIntervalMs);
                }
            }
        } catch (Exception e) {
            log.error("Failed to connect to Redis after {} attempts", i);
            throw e;
        }
    }
}
