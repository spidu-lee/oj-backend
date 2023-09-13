package com.ln.oj.utils;

import com.ln.oj.common.ErrorCode;
import com.ln.oj.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AIUtil {

    public static String chat(String content,String ai) {
        String res;
        if ("鱼聪明".equals(ai)) {
            res = chatByYu(content);
        } else if ("星火".equals(ai)) {
            res = chatByXh(content);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无该AI");
        }
        return res;
    }

    private static String chatByYu(String content) {

        String accessKey = "yj9ij3bjnzd01x3yik9higifq9xvc2ds";
        String secretKey = "c3qd7go1lix6otq5yeqfztivmzgjfpds";
        YuCongMingClient client = new YuCongMingClient(accessKey, secretKey);

        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(1672148191136464898L);
        devChatRequest.setMessage(content);
        BaseResponse<DevChatResponse> response = client.doChat(devChatRequest);
        return response.getData().getContent();
    }

    private static String chatByXh(String content) {
        SparkClient sparkClient = new SparkClient();

        // 设置认证信息
        sparkClient.appid = "102ce96f";
        sparkClient.apiKey = "b42e0303aacccdeeedc44f861cea5cee";
        sparkClient.apiSecret = "MDZkZjcxMWQ2YjZjNjllOGU4NWE0NmVh";

        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.userContent(content));

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
