package com.pcmall.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("product_hardware_specs")
public class ProductHardwareSpec {
    @TableId
    private Long productId;
    private String cpuSocket; private String memoryType; private String formFactor; private String supportedFormFactors;
    private Integer gpuLengthMm; private Integer caseGpuMaxLengthMm; private String storageInterface; private String supportedStorageInterfaces;
    private Integer psuWatt; private Integer estimatedPowerW;
    public Long getProductId() { return productId; } public void setProductId(Long productId) { this.productId = productId; }
    public String getCpuSocket() { return cpuSocket; } public void setCpuSocket(String cpuSocket) { this.cpuSocket = cpuSocket; }
    public String getMemoryType() { return memoryType; } public void setMemoryType(String memoryType) { this.memoryType = memoryType; }
    public String getFormFactor() { return formFactor; } public void setFormFactor(String formFactor) { this.formFactor = formFactor; }
    public String getSupportedFormFactors() { return supportedFormFactors; } public void setSupportedFormFactors(String supportedFormFactors) { this.supportedFormFactors = supportedFormFactors; }
    public Integer getGpuLengthMm() { return gpuLengthMm; } public void setGpuLengthMm(Integer gpuLengthMm) { this.gpuLengthMm = gpuLengthMm; }
    public Integer getCaseGpuMaxLengthMm() { return caseGpuMaxLengthMm; } public void setCaseGpuMaxLengthMm(Integer caseGpuMaxLengthMm) { this.caseGpuMaxLengthMm = caseGpuMaxLengthMm; }
    public String getStorageInterface() { return storageInterface; } public void setStorageInterface(String storageInterface) { this.storageInterface = storageInterface; }
    public String getSupportedStorageInterfaces() { return supportedStorageInterfaces; } public void setSupportedStorageInterfaces(String supportedStorageInterfaces) { this.supportedStorageInterfaces = supportedStorageInterfaces; }
    public Integer getPsuWatt() { return psuWatt; } public void setPsuWatt(Integer psuWatt) { this.psuWatt = psuWatt; }
    public Integer getEstimatedPowerW() { return estimatedPowerW; } public void setEstimatedPowerW(Integer estimatedPowerW) { this.estimatedPowerW = estimatedPowerW; }
}
