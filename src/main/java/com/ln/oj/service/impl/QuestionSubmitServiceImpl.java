package com.ln.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ln.oj.common.ErrorCode;
import com.ln.oj.constant.CommonConstant;
import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.core.judge.JudgeService;
import com.ln.oj.exception.BusinessException;
import com.ln.oj.mapper.QuestionSubmitMapper;
import com.ln.oj.model.dto.question.JudgeCase;
import com.ln.oj.model.dto.question.JudgeConfig;
import com.ln.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.ln.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.ln.oj.model.entity.Question;
import com.ln.oj.model.entity.QuestionSubmit;
import com.ln.oj.model.entity.User;
import com.ln.oj.model.enums.QuestionSubmitLanguageEnum;
import com.ln.oj.model.enums.QuestionSubmitStatusEnum;
import com.ln.oj.model.vo.QuestionSubmitVO;
import com.ln.oj.mq.MessageProducer;
import com.ln.oj.service.QuestionService;
import com.ln.oj.service.QuestionSubmitService;
import com.ln.oj.service.UserService;
import com.ln.oj.utils.AIUtil;
import com.ln.oj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author ln
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2023-08-09 16:52:08
 */
@Service
@Slf4j
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private JudgeService judgeService;

    @Resource
    private MessageProducer messageProducer;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        // 设置提交数
        Integer submitNum = question.getSubmitNum();
        Question updateQuestion = new Question();
        synchronized (question.getSubmitNum()) {
            submitNum = submitNum + 1;
            updateQuestion.setId(questionId);
            updateQuestion.setSubmitNum(submitNum);
            boolean save2 = questionService.updateById(updateQuestion);
            if (!save2) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据保存失败");
            }
        }
        Long questionSubmitId = questionSubmit.getId();
        // 发送消息
        String method = questionSubmitAddRequest.getMethod();
        if ("ai".equals(method)) {
             CompletableFuture.runAsync(() -> aiJudge(questionSubmitAddRequest, question, questionSubmit, questionSubmitId));
        } else {
            messageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));
        }

        return questionSubmit.getId();
    }

    private void aiJudge(QuestionSubmitAddRequest questionSubmitAddRequest, Question question, QuestionSubmit questionSubmit, Long questionSubmitId) {
        JudgeConfig judgeConfig = JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);
        Long timeLimit = judgeConfig.getTimeLimit();
        Long memoryLimit = judgeConfig.getMemoryLimit();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(question.getJudgeCase(), JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        List<String> outputList = judgeCaseList.stream().map(JudgeCase::getOutput).collect(Collectors.toList());
        String content = "假如你是"+questionSubmitAddRequest.getLanguage()+"代码执行器，能够记录代码执行时间和消耗内存，完全按照我以下的要求的进行，不要有额外的输出，不要任何多余的解释，要求如下：\n" +
                "代码执行时间取最大值给time，最大执行内存为memory，time和memory不需要单位，测试输入用例为："+inputList+"，预计输出为："+outputList+"，运行时间限制为"+timeLimit+"ms，运行内存限制为"+memoryLimit+"kb，判断代码正确性，如果超出时间限制则message为超时，如果超出内存限制则message为内存溢出，在都没有超出限制的前提下，如果实际输出和预计输出匹配则message为Accepted，不匹配则message为Wrong Answer，编译错误则message为编译错误\n" +
                "将以上信息封装进下面的json中，最终只返回该json信息，除此之外不要有任何多余的输出，xxxx为具体的执行信息，格式如下：\n" +
                "{\n" +
                "    \"message\": \"xxxx\",\n" +
                "    \"time\": \"xxxx\",\n" +
                "    \"memory\": \"xxxx\",\n" +
                "}\n" +
                "具体代码为：\n" +
                questionSubmitAddRequest.getCode();
        try {
            String res = AIUtil.chat(content, "鱼聪明");
            log.info("AI响应：{}",res);
            questionSubmit.setId(questionSubmitId);
            questionSubmit.setStatus(2);
            questionSubmit.setJudgeInfo(res);
            this.updateById(questionSubmit);
            JudgeInfo judgeInfo = JSONUtil.toBean(res, JudgeInfo.class);
            if ("Accepted".equals(judgeInfo.getMessage())) {
                // 设置通过数
                Long questionId = questionSubmit.getQuestionId();
                Integer acceptedNum = question.getAcceptedNum();
                Question updateQuestion = new Question();
                synchronized (question.getAcceptedNum()) {
                    acceptedNum = acceptedNum + 1;
                    updateQuestion.setId(questionId);
                    updateQuestion.setAcceptedNum(acceptedNum);
                    boolean save = questionService.updateById(updateQuestion);
                    if (!save) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存数据失败");
                    }
                }
            }
        } catch (Exception e) {
            questionSubmit.setJudgeInfo("{}");
            questionSubmit.setId(questionSubmitId);
            questionSubmit.setStatus(3);
            this.updateById(questionSubmit);
        }
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> {
                    QuestionSubmitVO questionSubmitVO = getQuestionSubmitVO(questionSubmit, loginUser);
                    questionSubmitVO.setUserName(userService.getById(questionSubmit.getUserId()).getUserName());
                    return questionSubmitVO;
                })
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }


}




