package com.erp.oms.advice;

import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.exception.BizException;
import com.erp.oms.exception.StockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 业务异常 ==========
    @ExceptionHandler(BizException.class)
    public ResultVO<Void> handleBizException(BizException ex) {
        return ResultVO.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(StockException.class)
    public ResultVO<Void> handleStockException(StockException ex) {
        return ResultVO.error(409, ex.getMessage());
    }

    // ========== 参数校验异常 ==========
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<Void> handleValidException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + "：" + e.getDefaultMessage())
                .findFirst()
                .orElse("参数错误");
        return ResultVO.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResultVO<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return ResultVO.error(400, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVO<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ResultVO.error(400, ex.getMessage());
    }

    // ========== 兜底异常 ==========
    @ExceptionHandler(Exception.class)
    public ResultVO<Void> handleException(Exception ex) {
        ex.printStackTrace();
        // log.error("系统异常", ex);
        return ResultVO.error(500, ex.getMessage() + "系统内部错误");
    }
}
