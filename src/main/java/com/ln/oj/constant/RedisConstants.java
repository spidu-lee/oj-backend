package com.ln.oj.constant;

public interface RedisConstants {

    //验证码
    String LOGIN_CODE_KEY = "user:login:user";
    Long LOGIN_CODE_TTL = 5L;

    //用户登录信息
    String LOGIN_USER_KEY = "user:msg:";
    Long LOGIN_USER_TTL = 60L;

    // AI答题记录
    String AI_ANSWER_KEY = "ai:answer:";
    Long AI_ANSWER_TTL = 10L;

    // 签到
    String USER_SIGN_KEY = "sign:";

    // 题目通过数
    String USER_ACCEPT_NUM = "user:accept";

}
