package com.ln.oj.core.judge.impl;

import cn.hutool.json.JSONUtil;
import com.ln.oj.common.ErrorCode;
import com.ln.oj.core.codesandbox.CodeSandbox;
import com.ln.oj.core.codesandbox.CodeSandboxFactory;
import com.ln.oj.core.codesandbox.CodeSandboxProxy;
import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;
import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.core.judge.JudgeManager;
import com.ln.oj.core.judge.JudgeService;
import com.ln.oj.core.judge.strategy.JudgeContext;
import com.ln.oj.exception.BusinessException;
import com.ln.oj.model.dto.question.JudgeCase;
import com.ln.oj.model.entity.Question;
import com.ln.oj.model.entity.QuestionSubmit;
import com.ln.oj.model.enums.QuestionSubmitStatusEnum;
import com.ln.oj.service.QuestionService;
import com.ln.oj.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ln
 */
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codeSandbox.type:example}")
    private String type;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目存在");
        }
        // 如果不为等待状态 TODO
        Integer questionSubmitStatus = questionSubmit.getStatus();
        if (!questionSubmitStatus.equals(QuestionSubmitStatusEnum.RUNNING.getValue()) && !questionSubmitStatus.equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "判题中");
        }
        // 更改状态为判题中，防止重复执行
        QuestionSubmit updateQuestionSubmit = new QuestionSubmit();
        updateQuestionSubmit.setId(questionSubmitId);
        updateQuestionSubmit.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(updateQuestionSubmit);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 调用代码沙箱
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = questionSubmit.getCode();
        String language = questionSubmit.getLanguage();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language(language)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        if ("AI服务异常".equals(executeCodeResponse.getMessage())) {
            updateQuestionSubmit = new QuestionSubmit();
            updateQuestionSubmit.setId(questionSubmitId);
            updateQuestionSubmit.setStatus(3);
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("AI服务异常");
            judgeInfo.setMemory(0L);
            judgeInfo.setTime(0L);
            updateQuestionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
            questionSubmitService.updateById(updateQuestionSubmit);
            return questionSubmitService.getById(questionSubmitId);
        }
        if (executeCodeResponse.getStatus() == 3) {
            updateQuestionSubmit = new QuestionSubmit();
            updateQuestionSubmit.setId(questionSubmitId);
            updateQuestionSubmit.setStatus(2);
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("编译错误");
            judgeInfo.setMemory(0L);
            judgeInfo.setTime(0L);
            updateQuestionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
            questionSubmitService.updateById(updateQuestionSubmit);
            return questionSubmitService.getById(questionSubmitId);
        }
        List<String> outputList = executeCodeResponse.getOutputList();
        //根据执行结果，设置判题状态
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setQuestion(question);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 修改判题结果
        updateQuestionSubmit = new QuestionSubmit();
        updateQuestionSubmit.setId(questionSubmitId);
        updateQuestionSubmit.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        updateQuestionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionSubmitService.updateById(updateQuestionSubmit);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        return questionSubmitService.getById(questionSubmitId);
    }
}
