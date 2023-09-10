package com.ln.oj.core.judge.strategy;

import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.model.dto.question.JudgeCase;
import com.ln.oj.model.entity.Question;
import com.ln.oj.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 * @author ln
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private Question question;

    private List<JudgeCase> judgeCaseList;

    private QuestionSubmit questionSubmit;

}
