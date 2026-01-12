package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 基础的增删改查已经由 BaseMapper 提供了
    // 如果需要根据用户名登录，直接调用：
    // this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, name));
}