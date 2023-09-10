package com.ln.oj.utils;


import com.ln.oj.common.ErrorCode;
import com.ln.oj.exception.BusinessException;
import com.ln.oj.model.entity.User;
import com.ln.oj.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * 腾讯云上传工具类
 */
@Slf4j
@Component
public class CosUtil {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CosManager cosManager;

    public String upload(MultipartFile file, HttpServletRequest request) {
        String finalUrl;
        int dotPos = file.getOriginalFilename().lastIndexOf(".");
        if (dotPos < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件错误");
        }
        User userVO = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + file.getOriginalFilename();
        String filepath = String.format("/%s/%s/%s", "oj" ,userVO.getId(), filename);
        File temp = null;
        try {
            // 上传文件
            temp = File.createTempFile(filepath, null);
            file.transferTo(temp);
            cosManager.putObject(filepath, temp);
            // 返回可访问地址
            finalUrl = "https://ln-1256621408.cos.ap-shanghai.myqcloud.com" + filepath;

            Long userId = userVO.getId();
            User user = userService.getById(userId);
            user.setUserAvatar(finalUrl);
            boolean update = userService.updateById(user);
            if (update) {
                //修改登录信息里的头像 TODO
//                stringRedisTemplate.opsForHash().put(UserConstant.USER_LOGIN_STATE + request.getSession().getId(), "userAvatar", finalUrl);
            }

        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (temp != null) {
                // 删除临时文件
                boolean delete = temp.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
        return finalUrl;
    }

}
