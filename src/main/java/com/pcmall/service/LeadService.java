package com.pcmall.service;
import com.pcmall.dto.LeadCreateRequest;
import com.pcmall.vo.LeadResponse;
public interface LeadService { LeadResponse create(LeadCreateRequest request, String clientIp); }
