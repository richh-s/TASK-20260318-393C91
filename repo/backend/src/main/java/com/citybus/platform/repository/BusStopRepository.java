package com.citybus.platform.repository;

import com.citybus.platform.entity.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusStopRepository extends JpaRepository<BusStop, Long> {

    List<BusStop> findByRouteIdOrderBySequenceNumber(Long routeId);

    Optional<BusStop> findByRouteIdAndNameEn(Long routeId, String nameEn);

    @Query("SELECT s FROM BusStop s WHERE " +
           "LOWER(s.nameEn) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.nameCn) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.pinyin) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.pinyinInitials) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<BusStop> searchStops(@Param("q") String q);

    @Query("SELECT DISTINCT s FROM BusStop s WHERE " +
           "(LOWER(s.nameEn) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(s.nameCn) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(s.pinyin) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(s.pinyinInitials) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND s.route.id = :routeId")
    List<BusStop> searchStopsInRoute(@Param("q") String q, @Param("routeId") Long routeId);

    @Query("SELECT DISTINCT s.nameEn FROM BusStop s WHERE " +
           "LOWER(s.nameEn) LIKE LOWER(CONCAT(:q, '%')) ORDER BY s.nameEn")
    List<String> autocompleteNames(@Param("q") String q);
}
