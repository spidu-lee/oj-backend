package com.ln.oj.mq;

import cn.hutool.json.JSONUtil;
import com.ln.oj.common.ErrorCode;
import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.core.judge.JudgeService;
import com.ln.oj.exception.BusinessException;
import com.ln.oj.model.entity.Question;
import com.ln.oj.model.entity.QuestionSubmit;
import com.ln.oj.model.enums.QuestionSubmitStatusEnum;
import com.ln.oj.service.QuestionService;
import com.ln.oj.service.QuestionSubmitService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author ln
 */
@Component
@Slf4j
public class MessageConsumer {

    @Resource
    private JudgeService judgeService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionService questionService;

    @RabbitListener(queues = {"code_queue"},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage:{}",message);
        long questionSubmitId = Long.parseLong(message);
        try {
            judgeService.doJudge(questionSubmitId);
            QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
            String judgeInfoStr = questionSubmit.getJudgeInfo();
            JudgeInfo judgeInfo = JSONUtil.toBean(judgeInfoStr, JudgeInfo.class);
            if ("Accepted".equals(judgeInfo.getMessage())) {
                // 设置通过数
                Long questionId = questionSubmit.getQuestionId();
                Question question = questionService.getById(questionId);
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
            channel.basicAck(deliveryTag,false);
        } catch (IOException e) {
            // TODO 失败重试
            try {
                channel.basicNack(deliveryTag,false,false);
            } catch (IOException ex) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,ex.getMessage());
            }
        }
    }

}
