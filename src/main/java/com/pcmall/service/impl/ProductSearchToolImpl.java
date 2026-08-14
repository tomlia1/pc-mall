package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.ProductSearchTool;
import com.pcmall.vo.ProductSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductSearchToolImpl implements ProductSearchTool {
    private final ProductMapper productMapper;

    public ProductSearchToolImpl(ProductMapper productMapper) { this.productMapper = productMapper; }

    @Override
    public ProductSearchResult search(ProductSearchCriteria criteria) {
        if (criteria == null || !StringUtils.hasText(criteria.productType())) {
            return ProductSearchResult.none("未确定商品类型，不能执行商品检索。");
        }
        List<Product> strict = find(criteria, true);
        if (!strict.isEmpty()) {
            return ProductSearchResult.exact("符合已确定的商品类型、预算和用途条件。", strict);
        }
        if (hasPreference(criteria)) {
            List<Product> alternatives = find(criteria, false);
            if (!alternatives.isEmpty()) {
                return ProductSearchResult.alternative("未找到同时满足全部偏好的商品，已保留商品类型、预算和用途，展示相关备选。", alternatives);
            }
        }
        return ProductSearchResult.none("当前商品库没有符合已确定条件的在售商品。");
    }

    private List<Product> find(ProductSearchCriteria criteria, boolean includePreferences) {
        LambdaQueryWrapper<Product> wrapper = baseQuery(criteria);
        if (includePreferences) {
            if (StringUtils.hasText(criteria.keyword())) {
                String keyword = criteria.keyword().trim();
                wrapper.and(query -> query.like(Product::getName, keyword)
                        .or().like(Product::getTags, keyword)
                        .or().like(Product::getCpu, keyword)
                        .or().like(Product::getGpu, keyword));
            }
            criteria.tags().stream().filter(StringUtils::hasText)
                    .forEach(tag -> wrapper.like(Product::getTags, tag.trim()));
        }
        return productMapper.selectList(wrapper).stream().limit(6).toList();
    }

    private LambdaQueryWrapper<Product> baseQuery(ProductSearchCriteria criteria) {
        return new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
                .ge(criteria.minBudget() != null, Product::getPrice, criteria.minBudget())
                .le(criteria.maxBudget() != null, Product::getPrice, criteria.maxBudget())
                .eq(Product::getProductType, criteria.productType())
                .eq(StringUtils.hasText(criteria.category()), Product::getCategory, criteria.category())
                .orderByAsc(Product::getPrice)
                .orderByAsc(Product::getId);
    }

    private boolean hasPreference(ProductSearchCriteria criteria) {
        return StringUtils.hasText(criteria.keyword()) || criteria.tags().stream().anyMatch(StringUtils::hasText);
    }
}
