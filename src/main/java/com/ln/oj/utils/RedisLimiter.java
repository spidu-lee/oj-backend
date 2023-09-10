package com.ln.oj.utils;

import com.ln.oj.common.ErrorCode;
import com.ln.oj.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author ln
 */
@Service
public class RedisLimiter {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     * @param key
     */
    public void doRateLimit(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 每5秒最多1次                                次    每
        rateLimiter.trySetRate(RateType.OVERALL,1,5, RateIntervalUnit.SECONDS);
        // 一个请求使用1个令牌
        boolean acquire = rateLimiter.tryAcquire(1);
        if (!acquire) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }

}
