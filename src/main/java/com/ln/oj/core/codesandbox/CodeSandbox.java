package com.ln.oj.core.codesandbox;

import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口
 * @author ln
 */
public interface CodeSandbox {

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);

}
