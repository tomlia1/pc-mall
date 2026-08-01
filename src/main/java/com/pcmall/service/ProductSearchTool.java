package com.pcmall.service;

import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.Product;
import java.util.List;

public interface ProductSearchTool {
    List<Product> search(ProductSearchCriteria criteria);
}
