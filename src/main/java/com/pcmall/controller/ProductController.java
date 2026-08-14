package com.pcmall.controller;

import com.pcmall.common.Result;
import com.pcmall.dto.ProductQuery;
import com.pcmall.entity.Product;
import com.pcmall.service.ProductService;
import com.pcmall.vo.ProductPage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }
    @GetMapping
    public Result<List<Product>> search(@RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(required = false) @Size(max = 64) String category,
            @RequestParam(required = false) @Size(max = 64) String brand,
            @RequestParam(required = false) @Size(max = 64) String keyword,
            @RequestParam(required = false) @Size(max = 64) String tag,
            @RequestParam(required = false) @jakarta.validation.constraints.Pattern(regexp = "LAPTOP|MINI_PC|CPU|MOTHERBOARD|GPU|RAM|SSD|PSU|CASE") String productType,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int pageSize,
            HttpServletResponse response) {
        ProductPage result = productService.search(new ProductQuery(minPrice, maxPrice, category, brand, keyword, tag, productType, page, pageSize));
        response.setHeader("X-Total-Count", String.valueOf(result.total()));
        response.setHeader("X-Total-Pages", String.valueOf(result.totalPages()));
        return Result.success(result.records());
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable @Min(1) long id) {
        return Result.success(productService.getActiveById(id));
    }
}
