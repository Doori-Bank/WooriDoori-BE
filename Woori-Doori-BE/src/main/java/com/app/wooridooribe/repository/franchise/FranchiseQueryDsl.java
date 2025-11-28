package com.app.wooridooribe.repository.franchise;

import com.querydsl.core.Tuple;

import java.util.List;

public interface FranchiseQueryDsl {
    // 가맹점명을 통한 가맹점 로고 조회
    List<Tuple> findFranchiseFilesByNames(List<String> historyNames);
}
