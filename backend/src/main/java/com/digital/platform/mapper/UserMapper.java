package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.digital.platform.common.request.AdminUserQuery;
import com.digital.platform.entity.User;
import com.digital.platform.vo.AdminUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper。
 * 继承 BaseMapper 后，单表增删改查无需写任何 SQL；
 * 复杂查询（多表关联、统计）再在 XML 或 @Select 里手写。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 管理后台用户列表（带每人发布的商品数）。
     * SQL 见 resources/mapper/UserMapper.xml
     */
    IPage<AdminUserVO> selectAdminUserPage(IPage<AdminUserVO> page, @Param("query") AdminUserQuery query);
}
