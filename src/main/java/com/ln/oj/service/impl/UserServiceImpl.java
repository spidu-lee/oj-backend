package com.ln.oj.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ln.oj.common.ErrorCode;
import com.ln.oj.constant.CommonConstant;
import com.ln.oj.constant.RedisConstants;
import com.ln.oj.exception.BusinessException;
import com.ln.oj.mapper.UserMapper;
import com.ln.oj.model.dto.user.UserLoginByPhoneRequest;
import com.ln.oj.model.dto.user.UserQueryRequest;
import com.ln.oj.model.entity.User;
import com.ln.oj.model.enums.UserRoleEnum;
import com.ln.oj.model.vo.LoginUserVO;
import com.ln.oj.model.vo.UserVO;
import com.ln.oj.service.UserService;
import com.ln.oj.utils.RedisLimiter;
import com.ln.oj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ln.oj.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/spidu-lee">ln</a>
 * @from <a href="https://spidu.cn">Java菜鸡窝</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisLimiter redisLimiter;


    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "oj";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            String username = "用户" + UUID.randomUUID().toString(true).substring(0,10);
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName(username);
            user.setUserAvatar("https://ln-1256621408.cos.ap-shanghai.myqcloud.com/oj/default_avatar.png");
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态 TODO
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public LoginUserVO userLoginByPhone(UserLoginByPhoneRequest userLoginByPhoneRequest, HttpServletRequest request) {
        String phone = userLoginByPhoneRequest.getPhone();
        String code = userLoginByPhoneRequest.getCode();
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (!code.equals(cacheCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"验证码错误");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", phone);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            User user = this.baseMapper.selectOne(queryWrapper);
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return this.getLoginUserVO(user);
        }
        User user = new User();
        user.setUserAccount(phone);
        String username = "用户" + UUID.randomUUID().toString(true).substring(0,10);
        user.setUserName(username);
        user.setUserAvatar("https://ln-1256621408.cos.ap-shanghai.myqcloud.com/oj/default_avatar.png");
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + "12345678").getBytes());
        user.setUserPassword(encryptPassword);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"登录失败");
        }
        //记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    @Override
    public void sign(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        redisLimiter.doRateLimit("sign_"+loginUser.getId());
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + loginUser.getId() + keySuffix;
        // 获取第几天
        int day = now.getDayOfMonth();
        stringRedisTemplate.opsForValue().setBit(key,day - 1,true);
        String userRole = loginUser.getUserRole();
        if ("user".equals(userRole)) {
            loginUser.setCoin(loginUser.getCoin() + 5);
        } else if ("vip".equals(userRole)) {
            loginUser.setCoin(loginUser.getCoin() + 20);
        }
        this.updateById(loginUser);
    }

    @Override
    public List<Integer> getSign(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + loginUser.getId() + keySuffix;
        int day = now.getDayOfMonth();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < day; i++) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i);
            if (bit == null) {
                return list;
            }
            if (bit) {
                list.add(1);
            } else {
                list.add(0);
            }
        }
        return list;

        // TODO 计算左边的0，补位

//        List<Long> signs = stringRedisTemplate.opsForValue()
//                .bitField(key, BitFieldSubCommands.create()
//                        .get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
//        if (signs == null || signs.isEmpty()) {
//            loginUserVO.setSignDays(ListUtil.toList(0));
//            return loginUserVO;
//        }
//        // 十进制
//        Long signNum = signs.get(0);
//        if (signNum == null || signNum == 0) {
//            loginUserVO.setSignDays(ListUtil.toList(0));
//            return loginUserVO;
//        }
//        // 将十进制数字转换为二进制字符串
//        String binaryString = Long.toBinaryString(signNum);
//        // 遍历二进制字符串的每一位，并将每一位添加到 List<Integer> 中
//        for (int i = 0; i < binaryString.length(); i++) {
//            char binaryChar = binaryString.charAt(i);
//            int binaryDigit = Character.getNumericValue(binaryChar);
//            list.add(binaryDigit);
//        }
//        loginUserVO.setSignDays(list);
//        return loginUserVO;
    }

}