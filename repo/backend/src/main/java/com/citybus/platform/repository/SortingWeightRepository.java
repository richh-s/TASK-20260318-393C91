package com.citybus.platform.repository;

import com.citybus.platform.entity.SortingWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SortingWeightRepository extends JpaRepository<SortingWeight, Long> {
    Optional<SortingWeight> findByFactorName(String factorName);
}
