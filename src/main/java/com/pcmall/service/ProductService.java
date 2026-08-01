package com.pcmall.service;
import com.pcmall.dto.ProductQuery;
import com.pcmall.entity.Product;
import java.util.List;
public interface ProductService {
    List<Product> search(ProductQuery query);
    Product getActiveById(long id);
}
