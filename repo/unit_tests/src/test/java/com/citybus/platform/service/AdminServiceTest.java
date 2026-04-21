package com.citybus.platform.service;

import com.citybus.platform.dto.request.*;
import com.citybus.platform.entity.*;
import com.citybus.platform.exception.BusinessException;
import com.citybus.platform.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock NotificationTemplateRepository templateRepository;
    @Mock SortingWeightRepository weightRepository;
    @Mock FieldDictionaryRepository dictRepository;
    @Mock SystemConfigRepository configRepository;

    @InjectMocks AdminService adminService;

    @Test
    void saveTemplate_existingName_updatesInPlace() {
        NotificationTemplate existing = new NotificationTemplate();
        existing.setId(1L);
        existing.setName("arrival");
        existing.setSensitivityLevel(0);

        when(templateRepository.findByName("arrival")).thenReturn(Optional.of(existing));
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemplateRequest req = new TemplateRequest();
        req.setName("arrival");
        req.setType(Notification.NotificationType.ARRIVAL_REMINDER);
        req.setTitleTemplate("Title");
        req.setContentTemplate("Content");
        req.setSensitivityLevel(2);

        NotificationTemplate saved = adminService.saveTemplate(req);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getSensitivityLevel()).isEqualTo(2);
        assertThat(saved.getContentTemplate()).isEqualTo("Content");
    }

    @Test
    void saveTemplate_newName_createsNew() {
        when(templateRepository.findByName("new")).thenReturn(Optional.empty());
        when(templateRepository.save(any())).thenAnswer(inv -> {
            NotificationTemplate t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        TemplateRequest req = new TemplateRequest();
        req.setName("new");
        req.setType(Notification.NotificationType.TASK_ASSIGNED);
        req.setTitleTemplate("New Task");
        req.setContentTemplate("Details");

        NotificationTemplate saved = adminService.saveTemplate(req);
        assertThat(saved.getId()).isEqualTo(99L);
    }

    @Test
    void deleteTemplate_missing_throwsNotFound() {
        when(templateRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> adminService.deleteTemplate(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteTemplate_existing_succeeds() {
        when(templateRepository.existsById(1L)).thenReturn(true);
        adminService.deleteTemplate(1L);
        verify(templateRepository).deleteById(1L);
    }

    @Test
    void saveWeight_upsertByFactorName() {
        SortingWeight existing = new SortingWeight();
        existing.setId(1L);
        existing.setFactorName("popularity_score");
        existing.setWeight(BigDecimal.ONE);

        when(weightRepository.findByFactorName("popularity_score")).thenReturn(Optional.of(existing));
        when(weightRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SortingWeightRequest req = new SortingWeightRequest();
        req.setFactorName("popularity_score");
        req.setWeight(BigDecimal.valueOf(2.5));

        SortingWeight saved = adminService.saveWeight(req);
        assertThat(saved.getWeight()).isEqualTo(BigDecimal.valueOf(2.5));
    }

    @Test
    void getDictionaries_withFieldName_filters() {
        FieldDictionary entry = new FieldDictionary();
        entry.setFieldName("stopStatus");
        when(dictRepository.findByFieldName("stopStatus")).thenReturn(List.of(entry));

        List<FieldDictionary> result = adminService.getDictionaries("stopStatus");
        assertThat(result).hasSize(1);
        verify(dictRepository, never()).findAll();
    }

    @Test
    void getDictionaries_blankFieldName_returnsAll() {
        when(dictRepository.findAll()).thenReturn(List.of());
        adminService.getDictionaries("");
        verify(dictRepository).findAll();
    }

    @Test
    void saveDictionary_upsertByFieldAndRawValue() {
        FieldDictionary existing = new FieldDictionary();
        existing.setId(1L);
        existing.setFieldName("stopStatus");
        existing.setRawValue("运营");
        existing.setStandardValue("ACTIVE");

        when(dictRepository.findByFieldNameAndRawValue("stopStatus", "运营"))
                .thenReturn(Optional.of(existing));
        when(dictRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FieldDictionaryRequest req = new FieldDictionaryRequest();
        req.setFieldName("stopStatus");
        req.setRawValue("运营");
        req.setStandardValue("OPERATIONAL");

        FieldDictionary saved = adminService.saveDictionary(req);
        assertThat(saved.getStandardValue()).isEqualTo("OPERATIONAL");
    }

    @Test
    void deleteDictionary_missing_throwsNotFound() {
        when(dictRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> adminService.deleteDictionary(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void saveConfig_upsertByConfigKey() {
        SystemConfig existing = new SystemConfig();
        existing.setId(1L);
        existing.setConfigKey("max_reservations");
        existing.setConfigValue("5");

        when(configRepository.findByConfigKey("max_reservations")).thenReturn(Optional.of(existing));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SystemConfigRequest req = new SystemConfigRequest();
        req.setConfigKey("max_reservations");
        req.setConfigValue("10");
        req.setDescription("Max daily reservations per user");

        SystemConfig saved = adminService.saveConfig(req);
        assertThat(saved.getConfigValue()).isEqualTo("10");
        assertThat(saved.getDescription()).isEqualTo("Max daily reservations per user");
    }

    @Test
    void getConfigValue_existingKey_returnsValue() {
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey("key");
        cfg.setConfigValue("value");
        when(configRepository.findByConfigKey("key")).thenReturn(Optional.of(cfg));
        assertThat(adminService.getConfigValue("key", "default")).isEqualTo("value");
    }

    @Test
    void getConfigValue_missingKey_returnsDefault() {
        when(configRepository.findByConfigKey("missing")).thenReturn(Optional.empty());
        assertThat(adminService.getConfigValue("missing", "fallback")).isEqualTo("fallback");
    }
}
