package com.app.wooridooribe.repository.franchise;

import com.app.wooridooribe.entity.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FranchiseRepository extends JpaRepository<Franchise, Long>, FranchiseQueryDsl {
}
