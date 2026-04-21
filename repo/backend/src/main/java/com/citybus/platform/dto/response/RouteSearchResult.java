package com.citybus.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RouteSearchResult {
    private Long id;
    private String routeNumber;
    private String name;
    private String description;
    private String status;
    private int stopCount;
}
