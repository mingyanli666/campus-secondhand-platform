package com.digital.platform.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.ChangePasswordDTO;
import com.digital.platform.dto.LoginDTO;
import com.digital.platform.dto.RegisterDTO;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.security.JwtUtil;
import com.digital.platform.service.impl.UserServiceImpl;
import com.digital.platform.vo.LoginVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务的单元测试。
 * <p>
 * 这里测的不是"能不能注册",而是几个容易被忽略、出事又很难查的地方：
 * <ul>
 *   <li><b>账号枚举</b> —— 账号不存在和密码错误必须返回同一个提示</li>
 *   <li><b>密码存储</b> —— 落库的必须是 BCrypt 密文，不能是明文</li>
 *   <li><b>禁用状态</b> —— 账号被禁用后不能再登录</li>
 *   <li><b>并发注册</b> —— 先查后插之间有窗口，要靠唯一索引兜底</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务 - 注册、登录、改密码")
class UserServiceImplTest {

    private static final long USER_ID = 12L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private FileService fileService;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeAll
    static void initTableInfo() {
        // register / login 里用了 LambdaQueryWrapper，需要 MyBatis-Plus 的元数据
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    // ============================================================
    //  注册
    // ============================================================

    @Test
    @DisplayName("账号已存在时拒绝注册")
    void register_rejects_duplicate_username() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertBusinessCode(() -> userService.register(registerDTO("zhangsan")),
                ResultCode.USERNAME_EXISTS);
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新注册的账号固定是普通用户，且默认启用、信誉分 100")
    void register_creates_enabled_normal_user() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register(registerDTO("newbie"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(RoleType.USER.getCode());
        assertThat(saved.getStatus()).isEqualTo(Constants.STATUS_ENABLED);
        assertThat(saved.getCreditScore()).isEqualTo(100);
        assertThat(saved.getNickname()).isEqualTo("newbie");   // 没填昵称就用账号
    }

    @Test
    @DisplayName("落库的是 BCrypt 密文，不是明文密码")
    void register_stores_hashed_password() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register(registerDTO("newbie"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());

        assertThat(captor.getValue().getPassword())
                .isNotEqualTo("123456")
                .startsWith("$2");   // BCrypt 密文固定前缀
    }

    @Test
    @DisplayName("并发注册同一账号时靠唯一索引兜底，翻译成友好提示")
    void register_translates_unique_key_conflict() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uk_username"));

        assertBusinessCode(() -> userService.register(registerDTO("newbie")),
                ResultCode.USERNAME_EXISTS);
    }

    // ============================================================
    //  登录
    // ============================================================

    @Test
    @DisplayName("账号不存在与密码错误返回同一个提示（防账号枚举）")
    void login_returns_same_error_for_unknown_user_and_wrong_password() {
        // ① 账号不存在
        when(userMapper.selectOne(any())).thenReturn(null);
        assertBusinessCode(() -> userService.login(loginDTO("ghost", "123456")),
                ResultCode.USERNAME_OR_PASSWORD_ERROR);

        // ② 密码错误 —— 错误码必须和上面完全一致，
        //    否则攻击者可以靠"提示不一样"判断出哪些账号真实存在
        User user = user("zhangsan", "$2a$10$abcdefghijklmnopqrstuv", 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        assertBusinessCode(() -> userService.login(loginDTO("zhangsan", "wrong")),
                ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }

    @Test
    @DisplayName("被禁用的账号不能登录")
    void login_rejects_disabled_user() {
        User user = user("linfeng", bcrypt("123456"), 0);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertBusinessCode(() -> userService.login(loginDTO("linfeng", "123456")),
                ResultCode.USER_DISABLED);
    }

    @Test
    @DisplayName("登录成功返回 token，并记录最后登录时间")
    void login_returns_token_and_updates_last_login() {
        User user = user("zhangsan", bcrypt("123456"), 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getNickname(), user.getRole()))
                .thenReturn("token-abc");

        LoginVO vo = userService.login(loginDTO("zhangsan", "123456"));

        assertThat(vo.getToken()).isEqualTo("token-abc");
        // 只更新 lastLoginTime 这一列，不重写整行
        verify(userMapper).updateById(org.mockito.ArgumentMatchers.argThat(u ->
                u.getLastLoginTime() != null && u.getPassword() == null));
    }

    // ============================================================
    //  改密码 / 查资料
    // ============================================================

    @Test
    @DisplayName("原密码不正确时拒绝修改")
    void changePassword_rejects_wrong_old_password() {
        User user = user("zhangsan", bcrypt("123456"), 1);
        when(userMapper.selectById(USER_ID)).thenReturn(user);

        assertBusinessCode(
                () -> userService.changePassword(USER_ID, passwordDTO("wrong", "newpass123")),
                ResultCode.OLD_PASSWORD_ERROR);
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("改密码后旧密码失效，新密码可用")
    void changePassword_replaces_hash() {
        User user = user("zhangsan", bcrypt("123456"), 1);
        when(userMapper.selectById(USER_ID)).thenReturn(user);

        userService.changePassword(USER_ID, passwordDTO("123456", "newpass123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        String newHash = captor.getValue().getPassword();

        assertThat(cn.hutool.crypto.digest.BCrypt.checkpw("newpass123", newHash)).isTrue();
        assertThat(cn.hutool.crypto.digest.BCrypt.checkpw("123456", newHash)).isFalse();
    }

    @Test
    @DisplayName("查询不存在的用户返回 USER_NOT_FOUND")
    void getProfile_throws_when_user_missing() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        assertBusinessCode(() -> userService.getProfile(USER_ID), ResultCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("更新资料不会把前端传来的 role / status 写进去")
    void updateProfile_ignores_sensitive_fields() {
        User user = user("zhangsan", bcrypt("123456"), 1);
        user.setRole(RoleType.USER.getCode());
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userService.updateProfile(USER_ID,
                profileDTO("新昵称", "13800000000", "a@b.com"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(RoleType.USER.getCode());
        assertThat(captor.getValue().getNickname()).isEqualTo("新昵称");
    }

    // ============================================================
    //  测试数据构造
    // ============================================================

    private RegisterDTO registerDTO(String username) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword("123456");
        return dto;
    }

    private LoginDTO loginDTO(String username, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private ChangePasswordDTO passwordDTO(String oldPwd, String newPwd) {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(oldPwd);
        dto.setNewPassword(newPwd);
        return dto;
    }

    private com.digital.platform.dto.UpdateProfileDTO profileDTO(String nickname,
                                                                 String phone, String email) {
        com.digital.platform.dto.UpdateProfileDTO dto =
                new com.digital.platform.dto.UpdateProfileDTO();
        dto.setNickname(nickname);
        dto.setPhone(phone);
        dto.setEmail(email);
        return dto;
    }

    private User user(String username, String passwordHash, int status) {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername(username);
        u.setNickname(username);
        u.setPassword(passwordHash);
        u.setRole(RoleType.USER.getCode());
        u.setStatus(status);
        u.setCreditScore(100);
        return u;
    }

    private String bcrypt(String raw) {
        return cn.hutool.crypto.digest.BCrypt.hashpw(raw,
                cn.hutool.crypto.digest.BCrypt.gensalt());
    }

    private void assertBusinessCode(org.junit.jupiter.api.function.Executable action,
                                    ResultCode expected) {
        assertThatThrownBy(action::execute)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(expected.getCode());
    }
}
