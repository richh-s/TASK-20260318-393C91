package com.citybus.platform.service;

import com.citybus.platform.dto.request.*;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final NotificationTemplateRepository templateRepository;
    private final SortingWeightRepository weightRepository;
    private final FieldDictionaryRepository dictRepository;
    private final SystemConfigRepository configRepository;

    // --- Templates ---

    public List<NotificationTemplate> getTemplates() {
        return templateRepository.findAll();
    }

    @Transactional
    public NotificationTemplate saveTemplate(TemplateRequest req) {
        NotificationTemplate tpl = templateRepository.findByName(req.getName())
                .orElse(new NotificationTemplate());
        tpl.setName(req.getName());
        tpl.setType(req.getType());
        tpl.setTitleTemplate(req.getTitleTemplate());
        tpl.setContentTemplate(req.getContentTemplate());
        if (req.getSensitivityLevel() != null) {
            tpl.setSensitivityLevel(req.getSensitivityLevel());
        }
        return templateRepository.save(tpl);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Template not found");
        }
        templateRepository.deleteById(id);
    }

    // --- Sorting Weights ---

    public List<SortingWeight> getWeights() {
        return weightRepository.findAll();
    }

    @Transactional
    public SortingWeight saveWeight(SortingWeightRequest req) {
        SortingWeight w = weightRepository.findByFactorName(req.getFactorName())
                .orElse(new SortingWeight());
        w.setFactorName(req.getFactorName());
        w.setWeight(req.getWeight());
        return weightRepository.save(w);
    }

    // --- Field Dictionary ---

    public List<FieldDictionary> getDictionaries(String fieldName) {
        if (fieldName != null && !fieldName.isBlank()) {
            return dictRepository.findByFieldName(fieldName);
        }
        return dictRepository.findAll();
    }

    @Transactional
    public FieldDictionary saveDictionary(FieldDictionaryRequest req) {
        FieldDictionary dict = dictRepository
                .findByFieldNameAndRawValue(req.getFieldName(), req.getRawValue())
                .orElse(new FieldDictionary());
        dict.setFieldName(req.getFieldName());
        dict.setRawValue(req.getRawValue());
        dict.setStandardValue(req.getStandardValue());
        return dictRepository.save(dict);
    }

    @Transactional
    public void deleteDictionary(Long id) {
        if (!dictRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Dictionary entry not found");
        }
        dictRepository.deleteById(id);
    }

    // --- System Config ---

    public List<SystemConfig> getConfigs() {
        return configRepository.findAll();
    }

    @Transactional
    public SystemConfig saveConfig(SystemConfigRequest req) {
        SystemConfig cfg = configRepository.findByConfigKey(req.getConfigKey())
                .orElse(new SystemConfig());
        cfg.setConfigKey(req.getConfigKey());
        cfg.setConfigValue(req.getConfigValue());
        cfg.setDescription(req.getDescription());
        return configRepository.save(cfg);
    }

    public String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }
}
