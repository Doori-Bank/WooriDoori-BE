package com.app.wooridooribe.service.auth;


import com.app.wooridooribe.controller.dto.*;
import com.app.wooridooribe.entity.*;
import com.app.wooridooribe.entity.type.*;
import com.app.wooridooribe.exception.*;
import com.app.wooridooribe.jwt.*;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.service.mailService.MailService;
import com.app.wooridooribe.service.token.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final RefreshTokenService refreshTokenService;  // Redis 기반으로 변경
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MailService mailService;

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> getMemberById(String email) {
        return memberRepository.findByMemberId(email);
    }

    @Override
    @Transactional
    public LoginResponseDto join(JoinDto joinDto) {
        try {
            // ✅ 1. 필수 요소 누락 확인
            if (joinDto.getId().isEmpty()|| joinDto.getPassword().isEmpty()||
                    joinDto.getName().isEmpty()|| joinDto.getPhone().isEmpty()) {
                throw new CustomException(ErrorCode.REQUIRED_MISSING);
            }

            // ✅ 2. 형식 검증 (이메일, 비밀번호 등)
            if (!joinDto.getId().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                throw new CustomException(ErrorCode.INVALID_FORMAT);
            }
            if (joinDto.getPassword().length() < 8) {
                throw new CustomException(ErrorCode.INVALID_FORMAT);
            }

            // ✅ 3. 중복 회원 확인
            if (memberRepository.findByMemberId(joinDto.getId()).isPresent()) {
                throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
            }

            // ✅ 4. 비밀번호 암호화 및 회원 저장
            String encodedPassword = passwordEncoder.encode(joinDto.getPassword());
            Member member = joinDto.toEntity(encodedPassword);
            memberRepository.save(member);

            log.info("회원가입 완료: {}", joinDto.getId());

            // ✅ 5. 자동 로그인
            return login(joinDto.getId(), joinDto.getPassword());


        } catch (CustomException e) {
            throw e; // 이미 정의된 예외는 그대로 던짐
        } catch (Exception e) {
            log.error("회원가입 중 서버 오류 발생", e);
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean checkId(String memberId) {

        Optional<Member> foundMember = memberRepository.findByMemberId(memberId);

        // 1. 비어있을 시
        if(memberId.isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        // 2. 형식 검증 (이메일, 비밀번호 등)
        if (!memberId.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new CustomException(ErrorCode.INVALID_FORMAT);
        }
        // 3. 이미 존재하는 ID인 경우 예외 발생
        if (foundMember.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        // 사용 가능한 IDw
        return true;
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

            // 4. RefreshToken Redis에 저장
            refreshTokenService.saveRefreshToken(memberId, tokenDto.getRefreshToken());

            // 5. LoginResponseDto 생성
            TokenRequestDto tokens = TokenRequestDto.builder()
                    .accessToken(tokenDto.getAccessToken())
                    .refreshToken(tokenDto.getRefreshToken())
                    .build();
            
            return LoginResponseDto.builder()
                    .name(memberDetail.getName())
                    .authority(memberDetail.getMember().getAuthority())
                    .tokens(tokens)
                    .build();
                    
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // 비밀번호가 틀린 경우
            log.error("로그인 실패 - 비밀번호 불일치: {}", memberId);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // 사용자를 찾을 수 없는 경우
            log.error("로그인 실패 - 사용자 없음: {}", memberId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        } catch (org.springframework.security.authentication.InternalAuthenticationServiceException e) {
            // MemberDetailService에서 던진 예외가 이 예외로 감싸질 수 있음
            if (e.getCause() instanceof UsernameNotFoundException) {
                log.error("로그인 실패 - 사용자 없음 (Internal): {}", memberId);
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            throw e;
        } catch (CustomException e) {
            // 계정 정지 등 이미 정의된 CustomException
            throw e;
        }
    }





    @Override
    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token null/empty 체크
        if (tokenRequestDto.getRefreshToken() == null || tokenRequestDto.getRefreshToken().isEmpty()) {
            throw new CustomException(ErrorCode.NO_TOKEN);
        }
        
        // 2. Refresh Token 검증 (만료, 서명 등)
        tokenProvider.validateToken(tokenRequestDto.getRefreshToken());

        // 3. Access Token에서 Member ID 가져오기
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());
        String memberId = authentication.getName();
        
        // 4. Redis에서 저장된 Refresh Token 조회
        String savedRefreshToken = refreshTokenService.getRefreshToken(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_REVOKED));

        // 5. Refresh Token 일치하는지 검사
        if (!savedRefreshToken.equals(tokenRequestDto.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 6. 새로운 토큰 생성
        MemberDetail memberDetail = (MemberDetail) authentication.getPrincipal();
        TokenDto tokenDto = tokenProvider.generateTokenDto(memberDetail);

        // 7. Redis에 새로운 Refresh Token 저장
        refreshTokenService.saveRefreshToken(memberId, tokenDto.getRefreshToken());

        return tokenDto;
    }

    @Override
    public Member findMemberByMemberNameAndPhone(MemberSearchIdDto memberSearchIdDto) {
        if(memberSearchIdDto.getName().isEmpty() || memberSearchIdDto.getPhone().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_FORMAT);
        }
        Optional<Member> member =  memberRepository.findByMemberNameAndPhone(memberSearchIdDto.getName(),memberSearchIdDto.getPhone());
        if(member.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        if(member.get().getStatus() == StatusType.UNABLE) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        return member.get();
    }
    
    @Override
    public void logout(String memberId) {
        // Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(memberId);
        log.info("로그아웃 완료: {}", memberId);
    }
    
    /**
     * 비밀번호 재설정 (임시 비밀번호 발급)
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        // 1. 필수 요소 검증
        if (resetPasswordDto.getMemberId() == null || resetPasswordDto.getMemberId().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        if (resetPasswordDto.getMemberName() == null || resetPasswordDto.getMemberName().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        
        // 2. 회원 조회 (이메일 + 이름으로 본인 확인)
        Member member = memberRepository.findByMemberId(resetPasswordDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 3. 이름이 일치하는지 확인
        if (!member.getMemberName().equals(resetPasswordDto.getMemberName())) {
            log.error("비밀번호 재설정 실패: 이름 불일치 - ID: {}, 입력된 이름: {}, 실제 이름: {}", 
                    resetPasswordDto.getMemberId(), resetPasswordDto.getMemberName(), member.getMemberName());
            throw new CustomException(ErrorCode.USER_NOT_FOUND); // 보안상 동일한 에러 메시지
        }
        
        // 4. 계정 상태 확인
        if (member.getStatus() == StatusType.UNABLE) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        
        // 5. 임시 비밀번호 생성 및 이메일 발송
        String tempPassword = mailService.sendTemporaryPassword(
                resetPasswordDto.getMemberId(), 
                resetPasswordDto.getMemberName()
        );
        log.info("임시 비밀번호 발급 및 이메일 발송 완료: {}", resetPasswordDto.getMemberId());
        
        // 6. DB에 임시 비밀번호 저장 (암호화)
        String encodedPassword = passwordEncoder.encode(tempPassword);
        member.setPassword(encodedPassword);
        memberRepository.save(member);
        
        // 저장 후 검증: 저장된 비밀번호가 올바르게 인코딩되었는지 확인
        Member savedMember = memberRepository.findByMemberId(resetPasswordDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean passwordMatches = passwordEncoder.matches(tempPassword, savedMember.getPassword());
        
        if (!passwordMatches) {
            log.error("비밀번호 재설정 후 검증 실패: {} - 임시 비밀번호와 저장된 비밀번호가 일치하지 않습니다.", resetPasswordDto.getMemberId());
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
        
        log.info("비밀번호 재설정 완료: {} - 비밀번호 검증 성공", resetPasswordDto.getMemberId());
    }
    
    /**
     * 비밀번호 변경 (기존 비밀번호 확인 후 변경)
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {
        // 1. 필수 요소 검증
        if (changePasswordDto.getMemberId() == null || changePasswordDto.getMemberId().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        if (changePasswordDto.getOldPassword() == null || changePasswordDto.getOldPassword().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        if (changePasswordDto.getNewPassword() == null || changePasswordDto.getNewPassword().isEmpty()) {
            throw new CustomException(ErrorCode.REQUIRED_MISSING);
        }
        
        // 2. 회원 조회
        Member member = memberRepository.findByMemberId(changePasswordDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 3. 계정 상태 확인
        if (member.getStatus() == StatusType.UNABLE) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        
        // 4. 기존 비밀번호 확인
        if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), member.getPassword())) {
            log.error("비밀번호 변경 실패: 기존 비밀번호 불일치 - ID: {}", changePasswordDto.getMemberId());
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 5. 새 비밀번호와 기존 비밀번호가 같은지 확인
        if (changePasswordDto.getOldPassword().equals(changePasswordDto.getNewPassword())) {
            log.error("비밀번호 변경 실패: 새 비밀번호가 기존 비밀번호와 동일 - ID: {}", changePasswordDto.getMemberId());
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }
        
        // 6. 새 비밀번호 암호화 및 저장
        String encodedNewPassword = passwordEncoder.encode(changePasswordDto.getNewPassword());
        member.setPassword(encodedNewPassword);
        memberRepository.save(member);
        
        // 저장 후 검증: 저장된 비밀번호가 올바르게 인코딩되었는지 확인
        Member savedMember = memberRepository.findByMemberId(changePasswordDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean passwordMatches = passwordEncoder.matches(changePasswordDto.getNewPassword(), savedMember.getPassword());
        
        if (!passwordMatches) {
            log.error("비밀번호 변경 후 검증 실패: {} - 새 비밀번호와 저장된 비밀번호가 일치하지 않습니다.", changePasswordDto.getMemberId());
            throw new CustomException(ErrorCode.SIGNIN_FAIL);
        }
        
        log.info("비밀번호 변경 완료: {} - 비밀번호 검증 성공", changePasswordDto.getMemberId());
    }

}