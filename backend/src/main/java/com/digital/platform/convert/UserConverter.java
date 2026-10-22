package com.digital.platform.convert;

import cn.hutool.core.bean.BeanUtil;
import com.digital.platform.entity.User;
import com.digital.platform.vo.UserVO;

/**
 * 实体 -> VO 转换。
 * <p>
 * 单独抽出来而不是在 Service 里到处 new，是为了"只改一处"：
 * 以后 UserVO 加字段，只改这里。
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        // 明确指定忽略 password，双保险
        return BeanUtil.copyProperties(user, UserVO.class, "password");
    }
}
