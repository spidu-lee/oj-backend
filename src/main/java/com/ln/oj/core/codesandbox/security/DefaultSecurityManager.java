package com.ln.oj.core.codesandbox.security;

import java.security.Permission;

/**
 * @author ln
 */
public class DefaultSecurityManager extends SecurityManager{

    /**
     * 检查所有权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做限制");
//        super.checkPermission(perm);
    }

}
