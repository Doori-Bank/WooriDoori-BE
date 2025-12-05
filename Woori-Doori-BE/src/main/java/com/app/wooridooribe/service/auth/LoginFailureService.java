package com.app.wooridooribe.service.auth;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 로그인 실패 처리 전용 서비스
 * REQUIRES_NEW로 별도 트랜잭션에서 실행하여 부모 트랜잭션의 롤백 영향을 받지 않음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginFailureService {

    private final MemberRepository memberRepository;

    /**
     * 로그인 실패 처리: 실패 횟수 증가 및 단계적 계정 잠금
     * - 10회 실패: 5분 잠금 (lockLevel 1)
     * - 다시 10회 실패: 10분 잠금 (lockLevel 2)
     * - 다시 10회 실패: 영구 잠금 (lockLevel 3+)
     * 
     * @Transactional(REQUIRES_NEW)로 별도 트랜잭션에서 실행하여 확실히 커밋
     */
    @Transactional(transactionManager = "db1TransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void handleLoginFailure(String memberId) {
        try {
            // DB에서 직접 조회하여 최신 상태 확인
            Optional<Member> memberOpt = memberRepository.findByMemberId(memberId);
            if (memberOpt.isEmpty()) {
                log.warn("로그인 실패 처리 - 사용자 없음: {}", memberId);
                return; // 사용자가 존재하지 않으면 실패 횟수 증가하지 않음
            }

            Member member = memberOpt.get();
            int currentLockLevel = (member.getLockLevel() == null ? 0 : member.getLockLevel());
            int currentFailedAttempts = (member.getFailedAttempts() == null ? 0 : member.getFailedAttempts());
            
            // 영구 잠금 상태면 더 이상 처리하지 않음 (lockLevel >= 3)
            if (currentLockLevel >= 3) {
                log.debug("로그인 실패 처리 - 이미 영구 잠금 상태: {}, lockLevel: {}", memberId, currentLockLevel);
                return;
            }
            
            // 실패 횟수 증가
            int newFailedAttempts = currentFailedAttempts + 1;
            
            // 실패 횟수가 10회에 도달할 때마다 잠금 레벨 증가
            int newLockLevel = currentLockLevel;
            LocalDateTime lockedUntil = member.getLockedUntil();
            
            if (newFailedAttempts == 10) {
                newLockLevel++;
                
                if (newLockLevel == 1) {
                    // 첫 번째 잠금: 5분
                    lockedUntil = LocalDateTime.now().plusMinutes(5);
                    log.warn("계정 잠금 처리 (1단계) - memberId: {}, 잠금 시간: 5분, 잠금 해제 시간: {}", 
                            memberId, lockedUntil);
                } else if (newLockLevel == 2) {
                    // 두 번째 잠금: 10분
                    lockedUntil = LocalDateTime.now().plusMinutes(10);
                    log.warn("계정 잠금 처리 (2단계) - memberId: {}, 잠금 시간: 10분, 잠금 해제 시간: {}", 
                            memberId, lockedUntil);
                } else if (newLockLevel >= 3) {
                    // 세 번째 이상 잠금: 영구 잠금 (lockedUntil을 매우 먼 미래로 설정)
                    lockedUntil = LocalDateTime.now().plusYears(100);
                    log.error("계정 영구 잠금 처리 - memberId: {}, 잠금 레벨: {}", memberId, newLockLevel);
                }
            }

            // Repository의 Native Query로 직접 UPDATE하여 DB에 확실히 반영
            int updatedRows = memberRepository.updateLoginFailure(memberId, newFailedAttempts, newLockLevel, lockedUntil);
            
            if (updatedRows > 0) {
                log.info("로그인 실패 횟수 증가 완료 (Repository Native Query) - memberId: {}, 이전 실패 횟수: {}, 현재 실패 횟수: {}, 잠금 레벨: {}, 업데이트된 행 수: {}", 
                        memberId, currentFailedAttempts, newFailedAttempts, newLockLevel, updatedRows);
            } else {
                log.warn("로그인 실패 횟수 증가 실패 - 업데이트된 행이 없음: {}", memberId);
            }
        } catch (Exception e) {
            log.error("로그인 실패 처리 중 오류 발생 - memberId: {}", memberId, e);
            // 예외를 다시 던져서 트랜잭션이 롤백되도록 함
            throw e;
        }
    }
}

