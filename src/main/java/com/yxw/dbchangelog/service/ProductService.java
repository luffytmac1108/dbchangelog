package com.yxw.dbchangelog.service;

import com.yxw.dbchangelog.mapper.ProductMapper;
import com.yxw.dbchangelog.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 更新产品信息并准备日志数据。
     * 整个方法都在一个事务中，如果任何一步失败，所有操作都会回滚。
     * 日志数据在事务成功后，由AOP发布事件异步处理。
     *
     * @param productId   要更新的产品ID
     * @param newPrice    新价格
     * @param newStock    新库存
     * @param updatedBy   操作人
     * @return 更新后的产品对象，如果更新失败抛出异常
     */
    @Transactional // 声明这是一个事务方法
    public Product updateProductAndPrepareLog(Long productId, BigDecimal newPrice, Integer newStock, String updatedBy) {
        // 1. 获取旧的产品信息 (用于日志记录和业务判断)
        Product oldProduct = productMapper.selectById(productId);
        if (oldProduct == null) {
            throw new IllegalArgumentException("Product with ID " + productId + " not found.");
        }
        oldProduct.setPrice(newPrice);
        oldProduct.setStock(newStock);
        oldProduct.setUpdatedBy(updatedBy);
        oldProduct.setLastUpdateTime(new Date());
        // 执行更新操作
        int rowsAffected = productMapper.updateProduct(oldProduct);
        if (rowsAffected == 0) {
            // 如果更新影响的行数为0，说明更新失败（例如ID不存在或数据未改变）
            // 抛出运行时异常，事务会自动回滚，日志也不会被记录
            throw new RuntimeException("Failed to update product with ID: " + productId);
        }
        return oldProduct;
    }

    /**
     * 模拟更新失败的场景，用于测试事务回滚和日志不记录。
     */
    @Transactional
    public Product updateProductAndPrepareLogSimulatingFailure(Long productId, BigDecimal newPrice, Integer newStock, String updatedBy) {
        Product oldProduct = productMapper.selectById(productId);
        if (oldProduct == null) {
            throw new IllegalArgumentException("Product with ID " + productId + " not found.");
        }
        oldProduct.setPrice(newPrice);
        oldProduct.setStock(newStock);
        oldProduct.setUpdatedBy(updatedBy);
        oldProduct.setLastUpdateTime(new Date());

        // 第一种情况，模拟没有数据被更新 mapper中的sql直接控制，比如 where id = -100
        //int rowsAffected = productMapper.updateProductSimulatingFailure(oldProduct);
        //if (rowsAffected == 0) {
        //    // 模拟业务失败，抛出异常，触发事务回滚
        //    throw new RuntimeException("Simulated failure: Product update did not affect any rows.");
        //}

        //第二种情况，直接业务逻辑异常
        int rowsAffected = productMapper.updateProduct(oldProduct);
        int i = 1 / 0;

        return oldProduct;
    }

    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    public List<Product> getAllProducts() {
        return productMapper.selectAll();
    }

    public Product createProduct(Product product) {
        product.setLastUpdateTime(new Date());
        productMapper.insertProduct(product);
        return product;
    }
}