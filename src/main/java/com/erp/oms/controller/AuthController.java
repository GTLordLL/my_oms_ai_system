package com.erp.oms.controller;

import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.sysUser.LoginDTO;
import com.erp.oms.dto.sysUser.LoginVO;
import com.erp.oms.service.sysUser.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResultVO<LoginVO> login(@RequestBody LoginDTO loginDto) {
        return ResultVO.success(sysUserService.login(loginDto));
    }
}