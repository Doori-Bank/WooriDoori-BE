package com.app.wooridooribe.service.fanchise;

import com.app.wooridooribe.controller.dto.FranchiseFileDto;

import java.util.List;

public interface FranchiseService {

    /** 카테고리별 가맹점 TOP5 조회 */
    List<FranchiseFileDto> getFranchise(Long memberId, String category);
}
