package com.erp.oms.service.sysUser;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.sysUser.LoginDTO;
import com.erp.oms.dto.sysUser.LoginVO;
import com.erp.oms.entity.SysUser;
import com.erp.oms.mapper.SysUserMapper;
import com.erp.oms.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor // 代替 @Autowired，构造器注入
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO loginDto) {
        // 1. 根据用户名查询用户信息
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDto.getUsername()));

        // 2. 校验用户是否存在及状态
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 3. 校验密码 (使用 BCrypt 匹配明文和数据库密文)
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        // 载荷中存入用户ID和角色，方便拦截器直接解析，不用查库
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());

        String token = JwtUtils.createToken(claims);

        // 5. 封装返回结果 VO
        return LoginVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .build();
    }
}