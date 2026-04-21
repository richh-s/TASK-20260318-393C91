package com.citybus.platform.controller;

import com.citybus.platform.common.ApiResponse;
import com.citybus.platform.dto.request.*;
import com.citybus.platform.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // Templates
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<?>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTemplates()));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<?>> saveTemplate(@Valid @RequestBody TemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.saveTemplate(req)));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<?>> deleteTemplate(@PathVariable Long id) {
        adminService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // Sorting weights
    @GetMapping("/weights")
    public ResponseEntity<ApiResponse<?>> getWeights() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getWeights()));
    }

    @PostMapping("/weights")
    public ResponseEntity<ApiResponse<?>> saveWeight(@Valid @RequestBody SortingWeightRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.saveWeight(req)));
    }

    // Field dictionary
    @GetMapping("/dictionaries")
    public ResponseEntity<ApiResponse<?>> getDictionaries(
            @RequestParam(required = false) String fieldName) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDictionaries(fieldName)));
    }

    @PostMapping("/dictionaries")
    public ResponseEntity<ApiResponse<?>> saveDictionary(@Valid @RequestBody FieldDictionaryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.saveDictionary(req)));
    }

    @DeleteMapping("/dictionaries/{id}")
    public ResponseEntity<ApiResponse<?>> deleteDictionary(@PathVariable Long id) {
        adminService.deleteDictionary(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // System config
    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<?>> getConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getConfigs()));
    }

    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<?>> saveConfig(@Valid @RequestBody SystemConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.saveConfig(req)));
    }
}
