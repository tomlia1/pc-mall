package com.pcmall.service.impl;

import com.pcmall.common.BusinessException;
import com.pcmall.dto.LeadCreateRequest;
import com.pcmall.entity.Lead;
import com.pcmall.entity.Product;
import com.pcmall.mapper.LeadMapper;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.security.RequestRateLimiter;
import com.pcmall.service.LeadService;
import com.pcmall.vo.LeadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LeadServiceImpl implements LeadService {
    private final LeadMapper leadMapper; private final ProductMapper productMapper; private final RequestRateLimiter rateLimiter;
    public LeadServiceImpl(LeadMapper leadMapper, ProductMapper productMapper, RequestRateLimiter rateLimiter) { this.leadMapper = leadMapper; this.productMapper = productMapper; this.rateLimiter = rateLimiter; }
    @Override public LeadResponse create(LeadCreateRequest request, String clientIp) {
        if (!Boolean.TRUE.equals(request.privacyAgreed())) throw new BusinessException(HttpStatus.BAD_REQUEST, "必须同意隐私说明后才能提交");
        if (!rateLimiter.tryAcquire("lead:" + clientIp, 3, Duration.ofDays(1))) throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "提交次数过多，请明日再试");
        Product product = productMapper.selectById(request.productId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) throw new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        Lead lead = new Lead();
        lead.setProductId(request.productId()); lead.setContactType(request.contactType()); lead.setContactValue(request.contactValue());
        lead.setPrivacyAgreed(true); lead.setAgreedAt(LocalDateTime.now()); lead.setUserBudget(request.userBudget()); lead.setUserRemark(request.userRemark()); lead.setIsDeleted(0);
        leadMapper.insert(lead);
        return new LeadResponse(lead.getId());
    }
}
