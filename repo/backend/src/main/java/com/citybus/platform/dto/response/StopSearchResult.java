package com.citybus.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StopSearchResult {
    private Long id;
    private String nameEn;
    private String nameCn;
    private String address;
    private Integer sequenceNumber;
    private Long routeId;
    private String routeNumber;
    private double popularityScore;
    private double sortScore;
}
