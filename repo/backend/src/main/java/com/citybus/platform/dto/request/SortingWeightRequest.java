package com.citybus.platform.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SortingWeightRequest {
    @NotBlank
    private String factorName;
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal weight;
}
