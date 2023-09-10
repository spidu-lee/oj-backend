package com.ln.oj.utils;

import cn.hutool.core.lang.UUID;

/**
 * @author ln
 */
public class UUIDTest {

    public static void main(String[] args) {
        String username = "用户" + UUID.randomUUID().toString(true).substring(0,10);
        System.out.println(username);
    }

}
