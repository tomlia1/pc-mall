package com.pcmall.dto;

public record BuildCheckRequest(Long cpuId, Long motherboardId, Long gpuId, Long ramId, Long ssdId, Long psuId, Long caseId) { }
