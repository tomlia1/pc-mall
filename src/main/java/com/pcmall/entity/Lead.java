package com.pcmall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("leads")
public class Lead {
    private Long id; private Long productId; private String contactType; private String contactValue; private Boolean privacyAgreed;
    private LocalDateTime agreedAt; private BigDecimal userBudget; private String userRemark; private Integer isDeleted; private LocalDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; } public void setProductId(Long productId) { this.productId = productId; }
    public String getContactType() { return contactType; } public void setContactType(String contactType) { this.contactType = contactType; }
    public String getContactValue() { return contactValue; } public void setContactValue(String contactValue) { this.contactValue = contactValue; }
    public Boolean getPrivacyAgreed() { return privacyAgreed; } public void setPrivacyAgreed(Boolean privacyAgreed) { this.privacyAgreed = privacyAgreed; }
    public LocalDateTime getAgreedAt() { return agreedAt; } public void setAgreedAt(LocalDateTime agreedAt) { this.agreedAt = agreedAt; }
    public BigDecimal getUserBudget() { return userBudget; } public void setUserBudget(BigDecimal userBudget) { this.userBudget = userBudget; }
    public String getUserRemark() { return userRemark; } public void setUserRemark(String userRemark) { this.userRemark = userRemark; }
    public Integer getIsDeleted() { return isDeleted; } public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
