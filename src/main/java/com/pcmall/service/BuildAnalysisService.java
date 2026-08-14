package com.pcmall.service;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.vo.BuildAnalysisResult;

public interface BuildAnalysisService {
    BuildAnalysisResult analyze(BuildCheckRequest request);
}
