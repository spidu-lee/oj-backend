package com.ln.oj.core.codesandbox.security;

/**
 * @author ln
 */
public class MySecurityManager extends SecurityManager {

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("禁止执行命令" + cmd);
    }

    @Override
    public void checkRead(String file) {
        if (file.contains("hutool")) {
            return;
        }
        throw new SecurityException("禁止读文件" + file);
    }

    @Override
    public void checkWrite(String file) {
        throw new SecurityException("禁止写入文件" + file);
    }

    @Override
    public void checkDelete(String file) {
        throw new SecurityException("禁止删除文件" + file);
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("禁止连接" + host + ":" + port);
    }
}
