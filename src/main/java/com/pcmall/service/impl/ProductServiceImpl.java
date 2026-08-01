package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.ProductQuery;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    public ProductServiceImpl(ProductMapper productMapper) { this.productMapper = productMapper; }
    @Override public List<Product> search(ProductQuery query) {
        if (query.minPrice() != null && query.maxPrice() != null && query.minPrice().compareTo(query.maxPrice()) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "minPrice 不能大于 maxPrice");
        }
        var wrapper = new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
                .eq(StringUtils.hasText(query.category()), Product::getCategory, query.category())
                .eq(StringUtils.hasText(query.brand()), Product::getBrand, query.brand())
                .ge(query.minPrice() != null, Product::getPrice, query.minPrice())
                .le(query.maxPrice() != null, Product::getPrice, query.maxPrice())
                .like(StringUtils.hasText(query.tag()), Product::getTags, query.tag())
                .and(StringUtils.hasText(query.keyword()), queryWrapper -> queryWrapper
                        .like(Product::getName, query.keyword())
                        .or().like(Product::getCpu, query.keyword())
                        .or().like(Product::getGpu, query.keyword()))
                .orderByAsc(Product::getPrice);
        return productMapper.selectPage(new Page<>(query.page(), query.pageSize()), wrapper).getRecords();
    }

    @Override public Product getActiveById(long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        }
        return product;
    }
}
