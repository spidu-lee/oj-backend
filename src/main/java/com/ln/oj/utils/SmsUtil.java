package com.ln.oj.utils;


import com.ln.oj.common.ErrorCode;
import com.ln.oj.exception.BusinessException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20190711.SmsClient;
import com.tencentcloudapi.sms.v20190711.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsUtil implements InitializingBean {

    /** 腾讯云账户密钥对secretKey（在访问管理中配置） */
    @Value("${tencent.sms.secretId}")
    private String secretID ;
    /** 腾讯云账户密钥对secretKey（在访问管理中配置） */
    @Value("${tencent.sms.keysecret}")
    private String secretKey ;
    @Value("${tencent.sms.smsSdkAppId}")
    private String smsSdkAppID ;
    @Value("${tencent.sms.signName}")
    private String signName ;
    @Value("${tencent.sms.templateId}")
    private String templateID ;

    public static String SECRET_ID;
    public static String SECRET_KEY;
    public static String SMSSDKAPP_ID;
    public static String SIGN_NAME;
    public static String TEMPLATE_ID;


    @Override
    public void afterPropertiesSet() {
        SECRET_ID = secretID;
        SECRET_KEY = secretKey;
        SMSSDKAPP_ID = smsSdkAppID;
        SIGN_NAME = signName;
        TEMPLATE_ID = templateID;
    }


    /**
     * 发送6位数字验证码到手机
     * @param code 验证码
     * @param phone 手机号
     * @return
     */
    public static void send(String code, String phone) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(SmsUtil.SECRET_ID, SmsUtil.SECRET_KEY);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的  第二个参数是地域信息
            SmsClient client = new SmsClient(cred, "ap-nanjing", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            SendSmsRequest req = new SendSmsRequest();

            //设置发送相关的参数
            String[] phoneNumberSet1 = {"86"+phone};
            req.setPhoneNumberSet(phoneNumberSet1);//发送的手机号
            //设置固定的参数
            req.setSmsSdkAppid(SmsUtil.SMSSDKAPP_ID);// 短信应用ID: 短信SdkAppId在 [短信控制台] 添加应用后生成的实际SdkAppId
            req.setSign(SmsUtil.SIGN_NAME);//短信签名内容: 使用 UTF-8 编码，必须填写已审核通过的签名
            req.setTemplateID(SmsUtil.TEMPLATE_ID);//模板 ID: 必须填写已审核通过的模板 ID
            //发送的验证码
            String[] templateParamSet1 = {code,"5"};//模板的参数 第一个是验证码，第二个是过期时间
            req.setTemplateParamSet(templateParamSet1);//发送验证码

            //发送短信
            // 返回的resp是一个SendSmsResponse的实例，与请求对象对应
            SendSmsResponse resp = client.SendSms(req);
            System.out.println("resp"+resp);
            // 输出json格式的字符串回包
            System.out.println(SendSmsResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"发送验证码失败");
        }
    }

}
