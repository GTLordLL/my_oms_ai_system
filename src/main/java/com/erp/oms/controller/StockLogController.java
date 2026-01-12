package com.erp.oms.controller;

import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.pageQuery.StockLogQueryDTO;
import com.erp.oms.dto.viewObject.StockLogVO;
import com.erp.oms.service.stockLog.StockLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "库存流水日志管理页")
@RestController
@RequestMapping("/api/stockLogs")
public class StockLogController {
    private final StockLogService stockLogService;

    public StockLogController(StockLogService stockLogService) {
        this.stockLogService = stockLogService;
    }

    @Operation(summary = "库存流水分页查询")
    @PostMapping("/page")
    public ResultVO<PageResultVO<StockLogVO>> pageStockLogs(@RequestBody @Validated StockLogQueryDTO query) {
        return ResultVO.success(stockLogService.pageByQuery(query));
    }
}
