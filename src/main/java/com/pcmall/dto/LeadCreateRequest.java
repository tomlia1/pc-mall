package com.pcmall.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LeadCreateRequest(@NotNull @Positive Long productId, @NotBlank @Pattern(regexp = "phone|weixin|email") String contactType,
                                @NotBlank @Size(max = 128) String contactValue, @NotNull Boolean privacyAgreed,
                                @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal userBudget,
                                @Size(max = 500) String userRemark) { }
