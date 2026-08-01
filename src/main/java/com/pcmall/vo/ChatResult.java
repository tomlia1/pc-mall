package com.pcmall.vo;

import com.pcmall.entity.Product;
import java.util.List;

public record ChatResult(String reply, List<Product> products) { }
