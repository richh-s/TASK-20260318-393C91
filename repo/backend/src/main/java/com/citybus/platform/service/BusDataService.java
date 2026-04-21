package com.citybus.platform.service;

import com.citybus.platform.dto.response.ImportResponse;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusDataService {

    private final BusDataImportRepository importRepository;
    private final BusRouteRepository routeRepository;
    private final BusStopRepository stopRepository;
    private final UserRepository userRepository;
    private final FieldDictionaryRepository dictRepository;
    private final ObjectMapper objectMapper;

    public Page<ImportResponse> listImports(Pageable pageable) {
        return importRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional
    public ImportResponse upload(MultipartFile file, String importType, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));

        BusDataImport.ImportType type;
        try {
            type = BusDataImport.ImportType.valueOf(importType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid import type: " + importType);
        }

        BusDataImport imp = new BusDataImport();
        imp.setFilename(file.getOriginalFilename());
        imp.setImportType(type);
        imp.setCreatedBy(user);
        imp.setStatus(BusDataImport.ImportStatus.PENDING);
        importRepository.save(imp);

        processAsync(imp.getId(), file, type);
        return toResponse(imp);
    }

    @Async
    public void processAsync(Long importId, MultipartFile file, BusDataImport.ImportType type) {
        BusDataImport imp = importRepository.findById(importId).orElseThrow();
        imp.setStatus(BusDataImport.ImportStatus.PARSING);
        importRepository.save(imp);

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            int[] counts;
            if (type == BusDataImport.ImportType.JSON) {
                counts = parseJson(content);
            } else {
                counts = parseHtml(content);
            }
            imp.setRowsParsed(counts[0]);
            imp.setRowsFailed(counts[1]);
            imp.setStatus(BusDataImport.ImportStatus.PARSED);
        } catch (Exception e) {
            imp.setStatus(BusDataImport.ImportStatus.FAILED);
            imp.setErrorMessage(e.getMessage());
            log.error("Import {} failed: {}", importId, e.getMessage());
        }
        imp.setCompletedAt(LocalDateTime.now());
        importRepository.save(imp);
    }

    private int[] parseJson(String content) throws Exception {
        List<Map<String, Object>> rows = objectMapper.readValue(content, new TypeReference<>() {});
        int parsed = 0, failed = 0;
        for (Map<String, Object> row : rows) {
            try {
                upsertStopFromMap(row);
                parsed++;
            } catch (Exception e) {
                log.warn("Failed to process row: {}", e.getMessage());
                failed++;
            }
        }
        return new int[]{parsed, failed};
    }

    private int[] parseHtml(String content) {
        int parsed = 0, failed = 0;
        Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Pattern cellPattern = Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Pattern tagPattern = Pattern.compile("<[^>]+>");
        Matcher rowMatcher = rowPattern.matcher(content);
        boolean firstRow = true;
        while (rowMatcher.find()) {
            if (firstRow) { firstRow = false; continue; }
            Matcher cellMatcher = cellPattern.matcher(rowMatcher.group(1));
            java.util.List<String> cells = new java.util.ArrayList<>();
            while (cellMatcher.find()) {
                cells.add(tagPattern.matcher(cellMatcher.group(1)).replaceAll("").trim());
            }
            if (cells.size() >= 3) {
                try {
                    String routeNum = cells.get(0);
                    String nameEn = cells.get(1);
                    String nameCn = cells.size() > 2 ? cells.get(2) : "";
                    BusRoute route = routeRepository.findByRouteNumber(routeNum)
                            .orElseGet(() -> createRoute(routeNum));
                    upsertStop(route, nameEn, nameCn, cells);
                    parsed++;
                } catch (Exception e) {
                    log.warn("HTML row parse failed: {}", e.getMessage());
                    failed++;
                }
            }
        }
        return new int[]{parsed, failed};
    }

    private void upsertStopFromMap(Map<String, Object> row) {
        String routeNum = normalize("route_number", getString(row, "routeNumber", getString(row, "route_number", null)));
        if (routeNum == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Missing routeNumber");
        BusRoute route = routeRepository.findByRouteNumber(routeNum)
                .orElseGet(() -> createRoute(routeNum));
        String nameEn = getString(row, "nameEn", getString(row, "name_en", "Stop"));
        String nameCn = getString(row, "nameCn", getString(row, "name_cn", ""));
        upsertStop(route, nameEn, nameCn, null);
    }

    private void upsertStop(BusRoute route, String nameEn, String nameCn, List<String> extraCells) {
        BusStop stop = stopRepository.findByRouteIdAndNameEn(route.getId(), nameEn)
                .orElse(new BusStop());
        stop.setRoute(route);
        stop.setNameEn(nameEn);
        stop.setNameCn(nameCn);
        if (stop.getSequenceNumber() == null) stop.setSequenceNumber(1);
        if (extraCells != null && extraCells.size() > 3) {
            try { stop.setAreaSqm(new BigDecimal(extraCells.get(3))); } catch (Exception ignored) {}
        }
        if (extraCells != null && extraCells.size() > 4) {
            try { stop.setPriceYuanMonth(new BigDecimal(extraCells.get(4))); } catch (Exception ignored) {}
        }
        stopRepository.save(stop);
    }

    private BusRoute createRoute(String routeNum) {
        BusRoute r = new BusRoute();
        r.setRouteNumber(routeNum);
        r.setName("Route " + routeNum);
        r.setStatus("ACTIVE");
        return routeRepository.save(r);
    }

    private String normalize(String fieldName, String rawValue) {
        if (rawValue == null) return null;
        return dictRepository.findByFieldNameAndRawValue(fieldName, rawValue)
                .map(FieldDictionary::getStandardValue)
                .orElse(rawValue);
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private ImportResponse toResponse(BusDataImport i) {
        return new ImportResponse(i.getId(), i.getImportType().name(), i.getStatus().name(),
                i.getFilename(), i.getRowsParsed(), i.getRowsFailed(), i.getCreatedAt());
    }
}
