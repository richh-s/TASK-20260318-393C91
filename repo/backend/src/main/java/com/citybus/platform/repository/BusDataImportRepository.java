package com.citybus.platform.repository;

import com.citybus.platform.entity.BusDataImport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusDataImportRepository extends JpaRepository<BusDataImport, Long> {
    Page<BusDataImport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
