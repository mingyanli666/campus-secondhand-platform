package com.digital.platform.service;

import com.digital.platform.dto.ChangePasswordDTO;
import com.digital.platform.dto.LoginDTO;
import com.digital.platform.dto.RegisterDTO;
import com.digital.platform.dto.UpdateProfileDTO;
import com.digital.platform.vo.LoginVO;
import com.digital.platform.vo.UserPublicVO;
import com.digital.platform.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口。
 * <p>
 * Controller 只依赖接口，实现类可以被替换（比如以后加缓存、加多数据源），
 * 这叫"面向接口编程"，是降低模块耦合的基本手段。
 */
public interface UserService {

    /** 注册新用户 */
    void register(RegisterDTO dto);

    /** 登录，返回 token 与用户信息 */
    LoginVO login(LoginDTO dto);

    /** 查询个人资料 */
    UserVO getProfile(Long userId);

    /**
     * 查询某个用户的公开资料（卖家主页用）。
     * <p>
     * 和 getProfile 的区别：这个方法返回的是"别人能看到的部分"，
     * 不含账号、手机、邮箱等隐私字段。
     */
    UserPublicVO getPublicProfile(Long userId);

    /** 修改个人资料 */
    UserVO updateProfile(Long userId, UpdateProfileDTO dto);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 上传头像并更新到用户资料 */
    String uploadAvatar(Long userId, MultipartFile file);
}
