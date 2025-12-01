package com.app.wooridooribe.repository.franchise;

import com.app.wooridooribe.entity.QFile;
import com.app.wooridooribe.entity.QFranchise;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FranchiseQueryDslImpl implements FranchiseQueryDsl{

    private final JPAQueryFactory queryFactory;

    /** 가맹점명을 통한 가맹점 로고 조회 */
    @Override
    public List<Tuple> findFranchiseFilesByNames(List<String> historyNames) {
        QFranchise franchise = QFranchise.franchise;
        QFile file = QFile.file;


        return queryFactory
                .select(
                        franchise.id,
                        franchise.franName,
                        file.id,
                        file.filePath
                )
                .from(franchise)
                .join(franchise.file, file)
                .where(franchise.franName.in(historyNames))
                .fetch();
    }
}
