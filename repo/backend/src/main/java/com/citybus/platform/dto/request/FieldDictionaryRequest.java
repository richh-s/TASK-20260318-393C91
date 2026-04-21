package com.citybus.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FieldDictionaryRequest {
    @NotBlank
    private String fieldName;
    @NotBlank
    private String rawValue;
    @NotBlank
    private String standardValue;
}
