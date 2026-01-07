package com.erp.oms.exception;

public class StockException extends BizException {
    public StockException(String message) {
        super(409, message);
    }

}
