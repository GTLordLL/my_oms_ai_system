package com.erp.oms.service.product;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.add.ProductCreateRequest;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.ProductQueryDTO;
import com.erp.oms.dto.update.ProductUpdateDTO;
import com.erp.oms.dto.viewObject.CompositeVO.ProductFullVO;
import com.erp.oms.dto.viewObject.ProductVO;
import com.erp.oms.entity.Product;

public interface ProductService extends IService<Product> {
    PageResultVO<ProductVO> pageByQuery(ProductQueryDTO query);
    ProductVO getByProductId(Long id);
    ProductFullVO getProductDetails(Long productId);
    void createProductFull(ProductCreateRequest request);
    void updateProductFull(ProductUpdateDTO dto);
    void safeDeleteProduct(Long productId);
}

