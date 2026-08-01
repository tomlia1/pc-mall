package com.pcmall.service.impl;

import com.pcmall.common.BusinessException;
import com.pcmall.dto.LeadCreateRequest;
import com.pcmall.entity.Lead;
import com.pcmall.entity.Product;
import com.pcmall.mapper.LeadMapper;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.security.RequestRateLimiter;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeadServiceImplTest {
    @Test
    void rejectsLeadWhenPrivacyConsentIsFalse() {
        LeadServiceImpl service = new LeadServiceImpl(mock(LeadMapper.class), mock(ProductMapper.class), new RequestRateLimiter());
        LeadCreateRequest request = new LeadCreateRequest(1L, "weixin", "wx_user", false, new BigDecimal("5000"), null);
        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request, "127.0.0.1"));
        assertEquals("必须同意隐私说明后才能提交", exception.getMessage());
    }

    @Test
    void persistsLeadAfterConsentAndActiveProductValidation() {
        LeadMapper leadMapper = mock(LeadMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        Product product = new Product(); product.setId(1L); product.setStatus(1);
        when(productMapper.selectById(1L)).thenReturn(product);
        when(leadMapper.insert(any(Lead.class))).thenAnswer(invocation -> { invocation.getArgument(0, Lead.class).setId(88L); return 1; });
        LeadServiceImpl service = new LeadServiceImpl(leadMapper, productMapper, new RequestRateLimiter());
        var result = service.create(new LeadCreateRequest(1L, "weixin", "wx_user", true, new BigDecimal("5000"), "请联系我"), "127.0.0.1");
        assertEquals(88L, result.id());
    }
}
