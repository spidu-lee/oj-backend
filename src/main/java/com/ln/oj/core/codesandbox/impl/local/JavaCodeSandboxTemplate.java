package com.ln.oj.core.codesandbox.impl.local;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.ln.oj.common.ErrorCode;
import com.ln.oj.core.codesandbox.CodeSandbox;
import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;
import com.ln.oj.core.codesandbox.model.ExecuteMessage;
import com.ln.oj.core.codesandbox.model.JudgeInfo;
import com.ln.oj.exception.BusinessException;
import com.ln.oj.utils.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Java代码沙箱模板方法
 * @author ln
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    public static final String GLOBAL_CODE_DIR_PATH = "temp";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 5000;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 1.把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        // 2.编译代码，得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        if (compileFileExecuteMessage.getExitValue() != 0) {
            // 获取错误信息
            log.error("编译错误：{}",compileFileExecuteMessage);
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setOutputList(new ArrayList<>());
            executeCodeResponse.setMessage(compileFileExecuteMessage.getErrorMessage());
            executeCodeResponse.setStatus(3);
            executeCodeResponse.setJudgeInfo(new JudgeInfo());
            // 清理文件
            boolean b = deleteFile(userCodeFile);
            if (!b) {
                log.error("删除文件失败：{}",userCodeFile.getAbsolutePath());
            }
            return executeCodeResponse;
        }

        log.info("编译成功：{}",compileFileExecuteMessage);

        // 3.执行代码
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile,inputList);

        // 4.整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5.文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("删除文件失败：{}",userCodeFile.getAbsolutePath());
        }

        return outputResponse;
    }


    /**
     * 1.把用户代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_PATH;
        // 判断代码目录是否存在
        if (FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 用户代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2.编译代码，得到class文件
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            return ProcessUtil.runProcess(compileProcess, "编译");
        } catch (IOException e) {
            // TODO
            // return getErrorResponse(e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"编译错误");
        }
    }

    /**
     * 3.执行文件，获得执行结果列表
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile,List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String s : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, s);

            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runProcess(runProcess, "运行");
                log.info("执行信息：{}",executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"执行错误");
            }
        }
        return executeMessageList;
    }

    /**
     * 4.获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            if (StrUtil.isNotBlank(executeMessage.getErrorMessage())) {
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
        }
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5.删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            log.info("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }


    /**
     * 6.获取错误响应
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
