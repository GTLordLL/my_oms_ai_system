package com.erp.oms.controller;

import com.erp.oms.dto.add.ProductCreateRequest;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.pageQuery.ProductQueryDTO;
import com.erp.oms.dto.update.ProductUpdateDTO;
import com.erp.oms.dto.viewObject.CompositeVO.ProductFullVO;
import com.erp.oms.dto.viewObject.ProductVO;
import com.erp.oms.service.product.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "商品管理页")
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "分页查询商品")
    @PostMapping("/page")
    // 1. 将 IPage<Product> 修改为 PageResult<ProductVO>
    public ResultVO<PageResultVO<ProductVO>> pageProducts(@RequestBody @Validated ProductQueryDTO query) {
        // 2. 调用 service，此时 service 的返回类型也应该是 PageResult<ProductVO>
        PageResultVO<ProductVO> pageResultVO = productService.pageByQuery(query);
        return ResultVO.success(pageResultVO);
    }

    @Operation(summary = "通过id查询商品")
    @GetMapping("/get/{id}")
    public ResultVO<ProductVO> getById(@PathVariable Long id) {
        ProductVO productVO = productService.getByProductId(id);
        return ResultVO.success(productVO);
    }

    @Operation(summary = "查询商品详情(含所有SKU)")
    @GetMapping("/details/{id}")
    public ResultVO<ProductFullVO> getProductDetails(@PathVariable Long id) {
        return ResultVO.success(productService.getProductDetails(id));
    }

    @Operation(summary = "商品上新")
    @PostMapping("/create")
    public ResultVO<Void> create(@Validated @RequestBody ProductCreateRequest request) {
        productService.createProductFull(request);
        return ResultVO.success();
    }

    @Operation(summary = "修改商品")
    @PutMapping("/update")
    public ResultVO<Void> update(@Validated @RequestBody ProductUpdateDTO dto){
        productService.updateProductFull(dto);
        return ResultVO.success();
    }

    @Operation(summary = "通过id删除商品")
    @DeleteMapping("/delete/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        productService.safeDeleteProduct(id);
        return ResultVO.success();
    }
}

