package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统用户表（后台管理人员）")
@TableName("oms_sys_user")
public class SysUser {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO) // 适配 PostgreSQL 的 SERIAL/BIGSERIAL
    private Long id;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "加密后的登录密码")
    private String password;

    @Schema(description = "用户昵称")
    private String nickname;

    /** * 角色标识
     * 建议在代码中定义常量类，例如：RoleConst.ADMIN, RoleConst.STAFF
     */
    @Schema(description = "角色标识：ADMIN-管理员, W_MANAGER-仓库经理, STAFF-普通员工")
    private String role;

    /** * 帐号状态
     * 1: 正常, 0: 禁用
     */
    @Schema(description = "帐号状态：1启用，0禁用")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT) // 如果配置了自动填充逻辑，下单或注册时会自动赋值
    private LocalDateTime createTime;
}