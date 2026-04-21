package com.citybus.platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_stops")
@Data
@NoArgsConstructor
public class BusStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private BusRoute route;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_cn")
    private String nameCn;

    @Column(name = "pinyin")
    private String pinyin;

    @Column(name = "pinyin_initials")
    private String pinyinInitials;

    private String address;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "apartment_type")
    private String apartmentType;

    @Column(name = "area_sqm")
    private BigDecimal areaSqm;

    @Column(name = "price_yuan_month")
    private BigDecimal priceYuanMonth;

    @Column(name = "popularity_score")
    private Integer popularityScore = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
