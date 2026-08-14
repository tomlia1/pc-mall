package com.pcmall.service;
import com.pcmall.dto.ProductQuery;
import com.pcmall.entity.Product;
import com.pcmall.vo.ProductPage;
public interface ProductService {
    ProductPage search(ProductQuery query);
    Product getActiveById(long id);
}
