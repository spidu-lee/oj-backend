package com.ln.oj.utils;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ln
 */
public class AITest {
    public static void main(String[] args) {

        String content = "假如你是java代码执行器，完全按照我以下的要求的进行，不要有额外的输出，不要任何多余的解释，要求如下：\n" +
                "\n" +
                "如果编译错误，将错误信息赋值给compileError\n" +
                "如果运行错误，将错误信息赋值给runError\n" +
                "如果没有编译和运行错误并且运行正常，将运行结果赋值给result\n" +
                "将输出信息封装进json中，格式如下：\n" +
                "{\n" +
                "    compileError: \"\",\n" +
                "    runError: \"\",\n" +
                "    result: \"\",\n" +
                "}\n" +
                "下面有一段Java程序，将代码编译，如果编译失败按照以上要求给出错误信息并停止执行，如果编译成功再运行，如果运行失败按照以上要求给出错误信息并停止执行，输入用例为：1 2，默认已经有导包，具体代码如下：\n" +
                "import java.util.Scanner;\n" +
                "\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        Scanner scanner = new Scanner(System.in);\n" +
                "        int a = scanner.nextInt();\n" +
                "        int b = scanner.nextInt();\n" +
                "        System.out.println(a + b);\n" +
                "    }\n" +
                "}";
        String chat = chat(content);
        System.out.println(chat);
    }

    private static String chat(String content) {
        SparkClient sparkClient = new SparkClient();

        // 设置认证信息
        sparkClient.appid = "102ce96f";
        sparkClient.apiKey = "b42e0303aacccdeeedc44f861cea5cee";
        sparkClient.apiSecret = "MDZkZjcxMWQ2YjZjNjllOGU4NWE0NmVh";

        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.userContent(content));
//        messages.add(SparkMessage.assistantContent("好的，这位同学，有什么问题需要李老师为你解答吗？"));
//        messages.add(SparkMessage.userContent("鲁迅和周树人小时候打过架吗？"));

        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                // 消息列表
                .messages(messages)
                // 模型回答的tokens的最大长度,非必传,取值为[1,4096],默认为2048
                .maxTokens(2048)
                // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
                // 指定请求版本，默认使用最新2.0版本
                .apiVersion(SparkApiVersion.V2_0)
                .build();

        // 同步调用
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        return chatResponse.getContent();
    }
}
