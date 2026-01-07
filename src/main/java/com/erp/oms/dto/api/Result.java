package com.erp.oms.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "统一接口返回")
public class Result<T> {

    private int code;        // 0 成功，非 0 失败
    private String message;  // 提示信息
    private T data;          // 泛型数据

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> error(int code,String message) {
        return new Result<>(code, message, null);
    }

}
