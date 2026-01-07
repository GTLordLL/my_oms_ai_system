package com.erp.oms.advice;

import com.erp.oms.dto.api.Result;
import com.erp.oms.exception.BizException;
import com.erp.oms.exception.StockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 业务异常 ==========
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(StockException.class)
    public Result<Void> handleStockException(StockException ex) {
        return Result.error(409, ex.getMessage());
    }

    // ========== 参数校验异常 ==========
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + "：" + e.getDefaultMessage())
                .findFirst()
                .orElse("参数错误");
        return Result.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return Result.error(400, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.error(400, ex.getMessage());
    }

    // ========== 兜底异常 ==========
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        ex.printStackTrace();
        // log.error("系统异常", ex);
        return Result.error(500, "系统内部错误");
    }
}
