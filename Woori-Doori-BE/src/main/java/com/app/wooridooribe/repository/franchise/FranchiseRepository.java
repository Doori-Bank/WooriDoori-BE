package com.app.wooridooribe.repository.franchise;

import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface FranchiseRepository extends JpaRepository<Franchise, Long>, FranchiseQueryDsl {
}
