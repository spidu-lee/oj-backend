package com.ln.oj.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ln
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient getRedissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(2);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
