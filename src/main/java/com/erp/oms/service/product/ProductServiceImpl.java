package com.erp.oms.service.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.add.ProductCreateRequest;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.ProductQueryDTO;
import com.erp.oms.dto.update.ProductUpdateDTO;
import com.erp.oms.dto.update.SkuUpdateDTO;
import com.erp.oms.dto.viewObject.CompositeVO.ProductFullVO;
import com.erp.oms.dto.viewObject.ProductVO;
import com.erp.oms.dto.viewObject.SkuVO;
import com.erp.oms.entity.Product;
import com.erp.oms.entity.Sku;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.mapper.ProductMapper;
import com.erp.oms.service.sku.SkuService;
import com.erp.oms.service.stockLog.StockLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品中心实现：管理 Spu（商品主表）及其关联的 Sku（规格属性）
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    private final SkuService skuService;
    private final StockLogService stockLogService;

    public ProductServiceImpl(SkuService skuService, StockLogService stockLogService) {
        this.skuService = skuService;
        this.stockLogService = stockLogService;
    }

    /**
     * [查询] 商品分页：支持名称模糊、分类/状态精确匹配及时间筛选
     */
    @Override
    public PageResultVO<ProductVO> pageByQuery(ProductQueryDTO query){
        Page<Product> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();

        // 1. 组合筛选：名称(Like)、分类(Eq)、状态(Eq)
        if (StringUtils.hasText(query.getKeyword())) w.like(Product::getName, query.getKeyword());
        if (StringUtils.hasText(query.getCategory())) w.eq(Product::getCategory, query.getCategory());
        if (query.getStatus() != null) w.eq(Product::getStatus, query.getStatus());

        // 2. 时间跨度：GE(>=), LE(<=)
        if (query.getStartTime() != null) w.ge(Product::getCreateTime, query.getStartTime());
        if (query.getEndTime() != null) w.le(Product::getCreateTime, query.getEndTime());

        // 3. 双重排序确保分页结果不抖动
        w.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);

        IPage<Product> productPage = this.page(page, w);

        List<ProductVO> voList = productPage.getRecords().stream().map(product -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(product, vo);
            return vo;
        }).collect(Collectors.toList());

        return PageResultVO.of(productPage.getTotal(), voList);
    }

    /**
     * [查询] 通过id查询单个商品
     */
    @Override
    public ProductVO getByProductId(Long productId) {
        Product product = this.getById(productId);
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    /**
     * [查询] 通过id查询商品及本身的sku
     */
    @Override
    public ProductFullVO getProductDetails(Long productId) {
        ProductVO productVO = this.getByProductId(productId);
        List<SkuVO> skus = skuService.listByProductId(productId);

        ProductFullVO fullVO = new ProductFullVO();
        BeanUtils.copyProperties(productVO, fullVO);
        fullVO.setSkus(skus);

        return fullVO;
    }

    /**
     * [创建] 全量保存：Spu 主表 -> Sku 批量保存 -> 初始化库存流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void createProductFull(ProductCreateRequest request) {
        // 1. 落库商品主记录
        Product product = new Product();
        BeanUtils.copyProperties(request.getProduct(), product);
        product.setCreateTime(LocalDateTime.now());
        this.save(product);

        // 2. 映射 SKU 关系并批量保存
        List<Sku> skuList = request.getSkus().stream().map(dto -> {
            Sku sku = new Sku();
            BeanUtils.copyProperties(dto, sku);
            sku.setProductId(product.getId()); // 绑定父 ID
            return sku;
        }).collect(Collectors.toList());

        skuService.saveBatch(skuList);

        // 3. 库存初始化：仅针对有初始库存的 SKU 记账
        for (Sku sku : skuList) {
            if (sku.getStockQuantity() != null && sku.getStockQuantity() > 0) {
                stockLogService.recordLog(
                        sku.getId(),
                        sku.getStockQuantity(),
                        0, // 初始库存前置为 0
                        StockChangeType.NEW_PRODUCT_IN,
                        "INIT_" + product.getId(),
                        "SYSTEM"
                );
            }
        }
    }

    /**
     * [修改] 全量更新：更新基础信息，支持灵活增删改 SKU
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductFull(ProductUpdateDTO dto) {
        // 1. 更新商品主表
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        this.updateById(product);

        // 2. 处理 SKU 变更逻辑 (增、删、改)
        if (dto.getSkus() != null) {
            Long productId = dto.getId();

            // A. 获取数据库中该商品现有的所有 SKU ID
            List<Sku> existingSkus = skuService.list(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, productId));
            Set<Long> existingIds = existingSkus.stream().map(Sku::getId).collect(Collectors.toSet());

            // B. 分类前端传来的 SKU
            List<Sku> toInsert = new ArrayList<>();
            List<Sku> toUpdate = new ArrayList<>();
            Set<Long> incomingIds = new HashSet<>();

            for (SkuUpdateDTO skuDto : dto.getSkus()) {
                Sku sku = new Sku();
                BeanUtils.copyProperties(skuDto, sku);
                sku.setProductId(productId);
                sku.setStockQuantity(null); // 安全性：禁止直接修改库存

                if (sku.getId() == null) {
                    // 没有 ID，说明是新添加的行
                    sku.setStockQuantity(0); // 初始化库存为 0
                    toInsert.add(sku);
                } else {
                    // 有 ID，说明是旧数据更新
                    toUpdate.add(sku);
                    incomingIds.add(sku.getId());
                }
            }

            // C. 执行操作
            // 1. 新增
            if (!toInsert.isEmpty()) skuService.saveBatch(toInsert);
            // 2. 修改
            if (!toUpdate.isEmpty()) skuService.updateBatchById(toUpdate);
            // 3. 删除 (数据库有但前端没传的 ID)
            List<Long> toDeleteIds = existingIds.stream()
                    .filter(id -> !incomingIds.contains(id))
                    .collect(Collectors.toList());

            if (!toDeleteIds.isEmpty()) {
                // 校验：如果要删除的 SKU 还有库存，禁止删除
                for (Long delId : toDeleteIds) {
                    Sku s = skuService.getById(delId);
                    if (s.getStockQuantity() != null && s.getStockQuantity() > 0) {
                        throw new RuntimeException("规格 [" + s.getSpecs() + "] 仍有库存，禁止删除");
                    }
                }
                skuService.removeByIds(toDeleteIds);
            }
        }
    }

    /**
     * [删除] 安全下架：有库存禁止删除，删除时联动 SKU 逻辑删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void safeDeleteProduct(Long productId) {
        // 1. 强校验：检查是否还有剩余库存
        List<Sku> skus = skuService.list(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, productId));

        if (skus.stream().anyMatch(s -> s.getStockQuantity() != null && s.getStockQuantity() > 0)) {
            throw new RuntimeException("该商品规格仍有库存，请清空后再删除");
        }

        // 2. 级联逻辑删除（注意：此处依赖实体类的 @TableLogic 或全局配置）
        skuService.remove(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, productId));
        this.removeById(productId);
    }
}

