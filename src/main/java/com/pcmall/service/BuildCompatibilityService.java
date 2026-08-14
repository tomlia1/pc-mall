package com.pcmall.service;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.vo.BuildCheckResult;

public interface BuildCompatibilityService {
    BuildCheckResult check(BuildCheckRequest request);
}
