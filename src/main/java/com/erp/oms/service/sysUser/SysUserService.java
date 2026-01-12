package com.erp.oms.service.sysUser;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.sysUser.LoginDTO;
import com.erp.oms.dto.sysUser.LoginVO;
import com.erp.oms.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    /**
     * 用户登录接口
     */
    LoginVO login(LoginDTO loginDto);
}