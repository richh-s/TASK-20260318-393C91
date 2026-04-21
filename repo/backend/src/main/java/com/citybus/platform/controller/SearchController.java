package com.citybus.platform.controller;

import com.citybus.platform.common.ApiResponse;
import com.citybus.platform.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<?>> searchRoutes(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.searchRoutes(q)));
    }

    @GetMapping("/stops")
    public ResponseEntity<ApiResponse<?>> searchStops(@RequestParam String q,
                                                       @RequestParam(required = false) Long routeId) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.searchStops(q, routeId)));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<?>> autocomplete(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.autocomplete(q)));
    }
}
