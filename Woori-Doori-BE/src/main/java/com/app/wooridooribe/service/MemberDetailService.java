package com.app.wooridooribe.service;


import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public MemberDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByMemberId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if(member.getStatus() == StatusType.UNABLE) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        // MemberDetail 객체 생성 시 Member 엔티티를 전달하여 사용자 정보와 권한 정보를 포함
        return new MemberDetail(member);
    }
}