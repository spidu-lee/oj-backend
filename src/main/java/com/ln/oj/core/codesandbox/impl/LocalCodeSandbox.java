package com.ln.oj.core.codesandbox.impl;

import com.ln.oj.core.codesandbox.CodeSandbox;
import com.ln.oj.core.codesandbox.impl.local.JavaDockerCodeSandbox;
import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;

/**
 * 示例代码沙箱
 * @author ln
 */
public class LocalCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return new JavaDockerCodeSandbox().executeCode(executeCodeRequest);
    }
}
