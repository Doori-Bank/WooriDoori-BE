package com.app.wooridooribe.service.member;

import com.app.wooridooribe.controller.dto.MemberResponseDto;
import com.app.wooridooribe.entity.CategoryMember;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.categoryMember.CategoryMemberRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final CategoryMemberRepository categoryMemberRepository;
    private final CardHistoryRepository cardHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponseDto> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        
        return members.stream()
                .map(MemberResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponseDto getMemberByIdForAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        return MemberResponseDto.from(member);
    }

    @Override
    @Transactional
    public void updateEssentialCategories(Long memberId, java.util.List<CategoryType> essentialCategories) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 필수 카테고리 설정 삭제
        categoryMemberRepository.deleteByMember(member);

        // 새 필수 카테고리 설정 저장
        List<CategoryMember> newMappings = essentialCategories.stream()
                .map(cat -> CategoryMember.builder()
                        .member(member)
                        .categoryType(cat)
                        .build())
                .toList();
        categoryMemberRepository.saveAll(newMappings);

        // 해당 카테고리 결제 내역의 총지출 포함 여부를 미포함(N)으로 일괄 변경
        if (!essentialCategories.isEmpty()) {
            cardHistoryRepository.updateIncludeTotalByMemberAndCategories(memberId, essentialCategories, false);
        }
    }
}
