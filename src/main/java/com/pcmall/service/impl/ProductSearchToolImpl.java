package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.ProductSearchTool;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductSearchToolImpl implements ProductSearchTool {
    private final ProductMapper productMapper;

    public ProductSearchToolImpl(ProductMapper productMapper) { this.productMapper = productMapper; }

    @Override
    public List<Product> search(ProductSearchCriteria criteria) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
                .le(criteria.maxBudget() != null, Product::getPrice, criteria.maxBudget())
                .eq(StringUtils.hasText(criteria.category()), Product::getCategory, criteria.category())
                .orderByAsc(Product::getPrice);
        if (criteria.tags() != null) {
            criteria.tags().stream().filter(StringUtils::hasText).forEach(tag -> wrapper.like(Product::getTags, tag));
        }
        return productMapper.selectList(wrapper).stream().limit(6).toList();
    }
}
