package com.app.wooridooribe.batch.reader;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * 활성 유저 목록을 읽는 ItemReader
 * 3개월 내 로그인한 유저만 조회
 * @StepScope를 사용하여 각 Step 실행마다 새로운 인스턴스 생성
 */
@Slf4j
public class MemberItemReader implements ItemReader<Member> {

    private MemberRepository memberRepository;
    
    private Iterator<Member> memberIterator;
    private boolean initialized = false;
    private List<Member> activeMembers;
    
    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Member read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        log.info("MemberItemReader.read() 호출됨 - initialized: {}", initialized);
        
        // 첫 호출 시 활성 유저 목록 조회
        if (!initialized) {
            log.info("MemberItemReader 초기화 시작");
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            activeMembers = memberRepository.findMembersLoggedInWithinThreeMonths(threeMonthsAgo);
            log.info("배치 점수 계산 시작 - 활성 유저 수: {}", activeMembers != null ? activeMembers.size() : 0);
            
            if (activeMembers == null || activeMembers.isEmpty()) {
                log.warn("활성 유저가 없습니다!");
                initialized = true;
                return null;
            }
            
            memberIterator = activeMembers.iterator();
            initialized = true;
            log.info("MemberItemReader 초기화 완료 - 유저 수: {}", activeMembers.size());
        }

        // 다음 유저 반환 (없으면 null 반환하여 종료)
        if (memberIterator != null && memberIterator.hasNext()) {
            Member member = memberIterator.next();
            log.info("Member 반환 - memberId: {}", member.getId());
            return member;
        }

        log.info("더 이상 읽을 데이터가 없음");
        return null; // 모든 데이터 처리 완료
    }
}

