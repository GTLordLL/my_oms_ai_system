package com.erp.oms.exception;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // 常用快捷方法
    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }
}
