package com.tangluck.p1;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductPackageRepository extends JpaRepository<ProductPackage, Long> {
    List<ProductPackage> findByStatusOrderBySortOrderAscPriceAmountAsc(String status);

    Optional<ProductPackage> findByPackageCodeAndStatus(String packageCode, String status);

    Optional<ProductPackage> findByPackageCode(String packageCode);
}
