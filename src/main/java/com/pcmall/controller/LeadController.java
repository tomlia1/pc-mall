package com.pcmall.controller;

import com.pcmall.common.Result;
import com.pcmall.dto.LeadCreateRequest;
import com.pcmall.service.LeadService;
import com.pcmall.util.ClientIpResolver;
import com.pcmall.vo.LeadResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {
    private final LeadService leadService;
    public LeadController(LeadService leadService) { this.leadService = leadService; }
    @PostMapping
    public ResponseEntity<Result<LeadResponse>> create(@Valid @RequestBody LeadCreateRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(leadService.create(request, ClientIpResolver.resolve(httpRequest))));
    }
}
