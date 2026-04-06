package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.CityReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityReferenceRepository extends JpaRepository<CityReference, Long> {
    Optional<CityReference> findByCityCode(String cityCode);
    Optional<CityReference> findByCityNameIgnoreCaseAndCountryIgnoreCase(String cityName, String country);
}
