package com.ln.oj.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author <a href="https://github.com/spidu-lee">ln</a>
 * @from <a href="https://spidu.cn">Java菜鸡窝</a>
 */
@Data
public class UserLoginByPhoneRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String phone;

    private String code;
}
