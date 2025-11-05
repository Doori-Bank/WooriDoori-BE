package com.app.wooridooribe.service.auth;


import com.app.wooridooribe.controller.dto.JoinDto;
import com.app.wooridooribe.controller.dto.LoginResponseDto;
import com.app.wooridooribe.controller.dto.TokenDto;
import com.app.wooridooribe.controller.dto.TokenRequestDto;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.RefreshToken;
import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.jwt.TokenProvider;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.repository.refreshToken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> getMemberById(String email) {
        return memberRepository.findByMemberId(email);
    }

    @Override
    @Transactional
    public LoginResponseDto join(JoinDto joinDto) {
        // 이미 가입된 회원인지 확인
        if (memberRepository.findByMemberId(joinDto.getMemberId()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(joinDto.getPassword());
        
        // DTO를 Entity로 변환하여 저장
        Member member = joinDto.toEntity(encodedPassword);
        memberRepository.save(member);
        
        log.info("회원가입 완료: {}", joinDto.getMemberId());

        // 자동 로그인
        return login(joinDto.getMemberId(), joinDto.getPassword());
    }

    @Override
    @Transactional
    public LoginResponseDto login(String memberId, String password) {
        try {
            // 1. memberId + password 로 AuthenticationToken 생성
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(memberId, password);

            // 2. 실제로 검증 (사용자 비밀번호 체크)
            // authenticate 메서드가 실행이 될 때 MemberDetailService 의 loadUserByUsername 메서드가 실행됨
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            // 3. 인증 정보를 기반으로 JWT 토큰 생성
            MemberDetail memberDetail = (MemberDetail) authentication.getPrincipal();
            TokenDto tokenDto = tokenProvider.generateTokenDto(memberDetail);

            // 4. RefreshToken 저장
            saveRefreshToken(memberId, tokenDto.getRefreshToken());

            // 5. LoginResponseDto 생성
            return LoginResponseDto.builder()
                    .name(memberDetail.getName())
                    .token(tokenDto.getAccessToken())
                    .build();
                    
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token이 유효하지 않습니다.");
        }

        // 2. Access Token에서 Member ID 가져오기
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());
        
        // 3. 저장소에서 Member ID를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshToken = refreshTokenRepository.findByKey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다."));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }

        // 5. 새로운 토큰 생성
        MemberDetail memberDetail = (MemberDetail) authentication.getPrincipal();
        TokenDto tokenDto = tokenProvider.generateTokenDto(memberDetail);

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        return tokenDto;
    }

    private void saveRefreshToken(String memberId, String refreshTokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .key(memberId)
                .value(refreshTokenValue)
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}