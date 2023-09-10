package com.ln.oj.core.codesandbox.impl;

import cn.hutool.json.JSONUtil;
import com.ln.oj.core.codesandbox.CodeSandbox;
import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;
import com.ln.oj.core.codesandbox.model.ExecuteMessage;
import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.utils.AIUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author ln
 */
@Slf4j
public class AICodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String content = "假如你是"+language+"代码执行器，能够记录代码执行时间和消耗内存和程序退出码，完全按照我以下的要求的进行，不要有额外的输出，不要任何多余的解释，要求如下：\n" +
                "如果编译错误或者运行错误，错误信息为errorMessage\n" +
                "如果没有编译和运行错误并且运行正常，运行结果为message\n" +
                "程序最大执行时间为time，单位为ms\n" +
                "程序最大执行内存为memory，单位为KB\n" +
                "程序退出码为exitValue\n" +
                "将以上信息封装进json中，xxxx为具体的错误信息或者执行结果，没有则为空，格式如下：\n" +
                "{\n" +
                "    \"exitValue\": \"xxxx\"\n" +
                "    \"message\": \"xxxx\",\n" +
                "    \"errorMessage\": \"xxxx\",\n" +
                "    \"time\": \"xxxx\",\n" +
                "    \"memory\": \"xxxx\",\n" +
                "}\n" +
                "下面有一段Java程序，将代码编译，如果编译失败按照以上要求给出错误信息并停止执行，如果编译成功再运行，如果运行失败按照以上要求给出错误信息并停止执行，输入用例为："+inputList+"，如果有多组输入用例，对应的message之间用英文逗号隔开，默认已经有导包，具体代码如下：\n" +
                code;

        ExecuteCodeResponse executeCodeResponse =new ExecuteCodeResponse();
        try {
            String res = AIUtil.chat(content,"鱼聪明");
            log.info("AI响应：{}",res);
            ExecuteMessage executeMessage = JSONUtil.toBean(res, ExecuteMessage.class);
            Integer exitValue = executeMessage.getExitValue();
            String message = executeMessage.getMessage();
            String errorMessage = executeMessage.getErrorMessage();
            Long time = executeMessage.getTime();
            Long memory = executeMessage.getMemory();
            if (Optional.ofNullable(exitValue).orElse(0) == 0) {
                // 正常退出
                String[] split = message.split(",");
                executeCodeResponse.setOutputList(Arrays.asList(split));
                executeCodeResponse.setStatus(1);
                JudgeInfo judgeInfo = new JudgeInfo();
                judgeInfo.setMemory(Optional.ofNullable(time).orElse(0L));
                judgeInfo.setTime(Optional.ofNullable(memory).orElse(0L));
                executeCodeResponse.setJudgeInfo(judgeInfo);

            } else {
                // 异常退出
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
            }
        } catch (Exception e) {
            executeCodeResponse.setMessage("AI服务异常");
            executeCodeResponse.setStatus(3);
            return executeCodeResponse;
        }

        return executeCodeResponse;
    }
}
