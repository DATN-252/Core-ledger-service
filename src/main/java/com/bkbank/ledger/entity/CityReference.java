package com.bkbank.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "city_reference")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_code", nullable = false, unique = true, length = 32)
    private String cityCode;

    @Column(name = "city_name", nullable = false, length = 128)
    private String cityName;

    @Column(nullable = false, length = 64)
    private String country;

    @Column(nullable = false)
    private Integer population;

    private Double latitude;
    private Double longitude;
}
