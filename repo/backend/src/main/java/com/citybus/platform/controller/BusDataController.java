package com.citybus.platform.controller;

import com.citybus.platform.common.ApiResponse;
import com.citybus.platform.entity.User;
import com.citybus.platform.service.BusDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bus-data")
@RequiredArgsConstructor
public class BusDataController {

    private final BusDataService busDataService;

    @GetMapping("/imports")
    public ResponseEntity<ApiResponse<?>> listImports(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(busDataService.listImports(pageable)));
    }

    @PostMapping("/imports")
    public ResponseEntity<ApiResponse<?>> upload(@AuthenticationPrincipal User user,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(defaultValue = "JSON") String importType) {
        return ResponseEntity.ok(ApiResponse.ok(busDataService.upload(file, importType, user.getId())));
    }
}
