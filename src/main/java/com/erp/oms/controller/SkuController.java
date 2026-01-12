package com.erp.oms.controller;

import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.pageQuery.SkuQueryDTO;
import com.erp.oms.dto.stockManager.PurchaseInboundDTO;
import com.erp.oms.dto.stockManager.RefundInboundDTO;
import com.erp.oms.dto.update.SkuUpdateDTO;
import com.erp.oms.dto.viewObject.SkuVO;
import com.erp.oms.service.sku.SkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sku管理页")
@RestController
@RequestMapping("/api/skus")
public class SkuController {
    final private SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @Operation(summary = "分页查询Sku")
    @PostMapping("/page")
    public ResultVO<PageResultVO<SkuVO>> pageSkus(@RequestBody @Validated SkuQueryDTO query) {
        return ResultVO.success(skuService.pageByQuery(query));
    }

    @Operation(summary = "修改Sku")
    @PutMapping("/update")
    public ResultVO<Void> update(@Validated @RequestBody SkuUpdateDTO dto) {
        skuService.updateSku(dto);
        return ResultVO.success();
    }

    @Operation(summary = "通过id删除Sku")
    @DeleteMapping("/delete/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        skuService.safeDeleteSku(id);
        return ResultVO.success();
    }

    @Operation(summary = "手动采购补货入库")
    @PutMapping("/inbound/purchase")
    public ResultVO<Void> purchaseInbound(@Validated @RequestBody PurchaseInboundDTO dto) {
        skuService.handlePurchaseInbound(dto);
        return ResultVO.success();
    }

    @Operation(summary = "手动退货入库")
    @PutMapping("/inbound/refund")
    public ResultVO<Void> refundInbound(@Validated @RequestBody RefundInboundDTO dto) {
        skuService.handleRefundInbound(dto);
        return ResultVO.success();
    }

    @Operation(summary = "通过productId查询商品的sku")
    @GetMapping("/list/{productId}")
    public ResultVO<List<SkuVO>> listByProductId(@PathVariable Long productId) {
        return ResultVO.success(skuService.listByProductId(productId));
    }
}

