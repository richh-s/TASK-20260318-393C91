package com.citybus.platform.repository;

import com.citybus.platform.entity.FieldDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldDictionaryRepository extends JpaRepository<FieldDictionary, Long> {
    List<FieldDictionary> findByFieldName(String fieldName);
    Optional<FieldDictionary> findByFieldNameAndRawValue(String fieldName, String rawValue);
}
