package com.ln.oj.core.judge.strategy;

import com.ln.oj.core.codesandbox.model.JudgeInfo;

/**
 * @author ln
 */
public interface JudgeStrategy {

    JudgeInfo doJudge(JudgeContext judgeContext);

}
