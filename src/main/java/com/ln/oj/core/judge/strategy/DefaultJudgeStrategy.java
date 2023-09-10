package com.ln.oj.core.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.model.dto.question.JudgeCase;
import com.ln.oj.model.dto.question.JudgeConfig;
import com.ln.oj.model.entity.Question;
import com.ln.oj.model.enums.JudgeInfoMessageEnum;

import java.util.List;

/**
 * 默认判题策略
 * @author ln
 */
public class DefaultJudgeStrategy implements JudgeStrategy {
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();

        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;
        JudgeInfo info = new JudgeInfo();
        info.setMemory(memory);
        info.setTime(time);

        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();

        // TODO
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            info.setMessage(judgeInfoMessageEnum.getValue());
            return info;
        }
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            if (!judgeCase.getOutput().equals(outputList.get(i))) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                info.setMessage(judgeInfoMessageEnum.getValue());
                return info;
            }
        }
        // 判断题目限制
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long memoryLimit = judgeConfig.getMemoryLimit();
        Long timeLimit = judgeConfig.getTimeLimit();
        if (memory > memoryLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            info.setMessage(judgeInfoMessageEnum.getValue());
            return info;
        }
        if (time > timeLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            info.setMessage(judgeInfoMessageEnum.getValue());
            return info;
        }
        info.setMessage(judgeInfoMessageEnum.getValue());
        return info;
    }
}
