package com.ln.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 *
 * @author <a href="https://github.com/spidu-lee">ln</a>
 * @from <a href="https://spidu.cn">Java菜鸡窝</a>
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 题币
     */
    private Integer coin;

    /**
     * 通过题目数
     */
    private Integer acceptNum;

    /**
     * 用户角色：user/admin/vip
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}