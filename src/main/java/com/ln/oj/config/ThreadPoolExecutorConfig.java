package com.ln.oj.config;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ln
 */
//@Configuration
public class ThreadPoolExecutorConfig {

//    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        /**
         *
         * int corePoolSize, 核心线程数，
         * int maximumPoolSize, 最大线程数
         * long keepAliveTime, 空闲线程（非核心）存活时间
         * TimeUnit unit, 时间单位
         * BlockingQueue<Runnable> workQueue, 工作队列
         * ThreadFactory threadFactory, 线程工厂
         * RejectedExecutionHandler handler 拒绝策略
         *
         */
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程"+count);
                count++;
                return thread;
            }
        };
        return new ThreadPoolExecutor(
                2,4,100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),threadFactory
                );
    }

}
