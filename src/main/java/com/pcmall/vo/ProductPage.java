package com.pcmall.vo;

import com.pcmall.entity.Product;
import java.util.List;

public record ProductPage(List<Product> records, long total, long totalPages) { }
