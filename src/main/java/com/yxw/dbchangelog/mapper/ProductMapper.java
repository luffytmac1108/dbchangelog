package com.yxw.dbchangelog.mapper;

import com.yxw.dbchangelog.model.Product;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper {
    Product selectById(Long id);
    List<Product> selectAll();
    int updateProduct(Product product);
    int updateProductSimulatingFailure(Product product); // 模拟更新失败
    int insertProduct(Product product);
}
