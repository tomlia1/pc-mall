package com.pcmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("products")
public class Product {
    private Long id; private String name; private String brand; private String category; private BigDecimal price;
    private String cpu; private String gpu; private Integer memoryGb; private Integer storageGb; private String tags;
    private String coverImage; private Integer status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; } public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; } public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal price) { this.price = price; }
    public String getCpu() { return cpu; } public void setCpu(String cpu) { this.cpu = cpu; }
    public String getGpu() { return gpu; } public void setGpu(String gpu) { this.gpu = gpu; }
    public Integer getMemoryGb() { return memoryGb; } public void setMemoryGb(Integer memoryGb) { this.memoryGb = memoryGb; }
    public Integer getStorageGb() { return storageGb; } public void setStorageGb(Integer storageGb) { this.storageGb = storageGb; }
    public String getTags() { return tags; } public void setTags(String tags) { this.tags = tags; }
    public String getCoverImage() { return coverImage; } public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
