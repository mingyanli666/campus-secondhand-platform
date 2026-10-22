package com.digital.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.convert.UserConverter;
import com.digital.platform.dto.ChangePasswordDTO;
import com.digital.platform.dto.LoginDTO;
import com.digital.platform.dto.RegisterDTO;
import com.digital.platform.dto.UpdateProfileDTO;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.security.JwtUtil;
import com.digital.platform.service.FileService;
import com.digital.platform.service.UserService;
import com.digital.platform.vo.LoginVO;
import com.digital.platform.vo.UserPublicVO;
import com.digital.platform.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final FileService fileService;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 先查一次，给出友好的提示
        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // 密码加密存储：即使数据库被拖库，也拿不到明文密码
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(StrUtil.blankToDefault(dto.getNickname(), dto.getUsername()));
        user.setRole(RoleType.USER.getCode());
        user.setStatus(Constants.STATUS_ENABLED);
        user.setGender(0);
        user.setCreditScore(100);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同一账号时，靠数据库唯一索引兜底，比先查后插更可靠
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));

        // 账号不存在与密码错误返回同样的提示，避免被用来枚举账号
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!Constants.STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getNickname(), user.getRole());

        // 记录最后登录时间，单独 update 避免把整行数据重写一遍
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        return new LoginVO(token, UserConverter.toVO(user));
    }

    @Override
    public UserVO getProfile(Long userId) {
        return UserConverter.toVO(requireUser(userId));
    }

    @Override
    public UserPublicVO getPublicProfile(Long userId) {
        User user = requireUser(userId);

        UserPublicVO vo = new UserPublicVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setCampus(user.getCampus());
        vo.setIntro(user.getIntro());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreateTime(user.getCreateTime());

        // 卖家主页上最该给的两个数字：
        // 在卖多少（他现在有什么）、卖出过多少（他靠不靠谱，用成交量做信任背书）。
        // 这两个是"别人能看到的经营数据"，和信誉分一起构成买家判断的依据。
        vo.setOnSaleCount(productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getUserId, userId)
                .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())));
        vo.setSoldCount(productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getUserId, userId)
                .eq(Product::getStatus, ProductStatus.SOLD.getCode())));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = requireUser(userId);

        // 只允许改这些字段，角色、状态、信誉分等敏感字段绝不接受前端传入
        user.setNickname(StrUtil.blankToDefault(dto.getNickname(), user.getNickname()));
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        user.setCampus(dto.getCampus());
        user.setIntro(dto.getIntro());

        userMapper.updateById(user);
        return UserConverter.toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = requireUser(userId);
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        User update = new User();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        requireUser(userId);
        String url = fileService.uploadImage(file, "avatar");

        User update = new User();
        update.setId(userId);
        update.setAvatar(url);
        userMapper.updateById(update);

        return url;
    }

    /**
     * 查用户，查不到直接抛异常，避免每个方法都写一遍 null 判断。
     */
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }
}
