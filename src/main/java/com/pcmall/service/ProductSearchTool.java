package com.pcmall.service;

import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.vo.ProductSearchResult;

public interface ProductSearchTool {
    ProductSearchResult search(ProductSearchCriteria criteria);
}
