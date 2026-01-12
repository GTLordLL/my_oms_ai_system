package com.erp.oms.dto.sysUser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录成功返回对象")
public class LoginVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "角色标识")
    private String role;

    @Schema(description = "身份令牌 (JWT Token)")
    private String token;

    @Schema(description = "Token过期时间 (秒)")
    private Long expiresIn;
}