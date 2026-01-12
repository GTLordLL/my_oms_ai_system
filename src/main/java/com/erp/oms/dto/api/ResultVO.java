package com.erp.oms.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "统一接口返回")
public class ResultVO<T> {

    private int code;        // 0 成功，非 0 失败
    private String message;  // 提示信息
    private T data;          // 泛型数据

    // 很多时候“删除”或“更新”操作只需要告诉前端成功了，不需要回传数据。
    public static <T> ResultVO<T> success() {
        return new ResultVO<>(0, "success", null);
    }

    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(0, "success", data);
    }

    public static <T> ResultVO<T> error(int code, String message) {
        return new ResultVO<>(code, message, null);
    }

}
