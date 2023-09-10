package com.ln.oj.core.codesandbox;

import com.ln.oj.common.ErrorCode;
import com.ln.oj.core.codesandbox.impl.AICodeSandbox;
import com.ln.oj.core.codesandbox.impl.LocalCodeSandbox;
import com.ln.oj.core.codesandbox.impl.RemoteCodeSandbox;
import com.ln.oj.exception.BusinessException;

/**
 * 代码沙箱工厂
 * @author ln
 */
public class CodeSandboxFactory {

    public static CodeSandbox newInstance(String type) {
        switch (type) {
            case "local":
                return new LocalCodeSandbox();
            case "remote":
                return new RemoteCodeSandbox();
            case "ai":
                return new AICodeSandbox();
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }

}
