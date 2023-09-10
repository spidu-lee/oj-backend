package com.ln.oj.core.judge;


import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.core.judge.strategy.DefaultJudgeStrategy;
import com.ln.oj.core.judge.strategy.JavaLanguageJudgeStrategy;
import com.ln.oj.core.judge.strategy.JudgeContext;
import com.ln.oj.core.judge.strategy.JudgeStrategy;
import com.ln.oj.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
