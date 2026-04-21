package com.citybus.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ImportResponse {
    private Long id;
    private String importType;
    private String status;
    private String fileName;
    private Integer rowsParsed;
    private Integer rowsFailed;
    private LocalDateTime createdAt;
}
