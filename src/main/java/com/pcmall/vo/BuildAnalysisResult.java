package com.pcmall.vo;

import com.pcmall.entity.Product;
import java.util.List;

public record BuildAnalysisResult(BuildCheckResult check, String reply, List<Product> candidates, boolean aiUnavailable) { }
