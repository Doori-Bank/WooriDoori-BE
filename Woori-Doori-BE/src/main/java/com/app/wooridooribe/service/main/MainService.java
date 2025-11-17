package com.app.wooridooribe.service.main;

import com.app.wooridooribe.controller.dto.MainDto;

public interface MainService {
    /**
     * 메인 페이지 데이터를 조회합니다.
     * @return MainDto 메인 페이지 응답 데이터
     */
    MainDto getMainPageData(Long memberId);
}

