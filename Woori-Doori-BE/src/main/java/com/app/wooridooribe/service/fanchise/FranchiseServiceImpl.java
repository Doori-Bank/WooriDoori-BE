package com.app.wooridooribe.service.fanchise;

import com.app.wooridooribe.controller.dto.FranchiseFileDto;
import com.app.wooridooribe.entity.QCardHistory;
import com.app.wooridooribe.entity.QFile;
import com.app.wooridooribe.entity.QFranchise;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.franchise.FranchiseRepository;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FranchiseServiceImpl implements FranchiseService {

    private final CardHistoryRepository cardHistoryRepository;
    private final FranchiseRepository franchiseRepository;

    /** 카테고리별 가맹점 TOP5 조회 */
    @Override
    public List<FranchiseFileDto> getFranchise(Long memberId, String category){
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.withDayOfMonth(1);
        CategoryType categoryType = CategoryType.valueOf(category);
        QCardHistory history = QCardHistory.cardHistory;
        QFranchise franchise = QFranchise.franchise;
        QFile file = QFile.file;

        // 가맹점 이름 불러오기
        List<Tuple> result = cardHistoryRepository.getCategoryStroeByMemberAndDateRange(memberId, categoryType, startDate, endDate);
        List<String> historyNames = result.stream().map(t -> t.get(history.historyName)).toList();

        // 가맹점 이름에 따른 로고 조회
        List<Tuple> franchiseFiles = franchiseRepository.findFranchiseFilesByNames(historyNames);

        // DTO 변환
        return franchiseFiles.stream()
                .map(t -> FranchiseFileDto.builder()
                        .franchiseId(t.get(franchise.id))
                        .franName(t.get(franchise.franName))
                        .fileId(t.get(file.id))
                        .imageUrl(t.get(file.filePath))
                        .build()
                )
                .toList();
    }
}
