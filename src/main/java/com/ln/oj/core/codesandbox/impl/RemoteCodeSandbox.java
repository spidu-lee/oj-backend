package com.ln.oj.core.codesandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ln.oj.common.ErrorCode;
import com.ln.oj.core.codesandbox.CodeSandbox;
import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;
import com.ln.oj.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

/**
 * 远程代码沙箱
 * @author ln
 */
public class RemoteCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String url = "http://localhost:8754/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        String res = HttpUtil.createPost(url).body(json).execute().body();
        if (StringUtils.isBlank(res)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"调用接口失败："+res);
        }
        return JSONUtil.toBean(res,ExecuteCodeResponse.class);
    }
}
