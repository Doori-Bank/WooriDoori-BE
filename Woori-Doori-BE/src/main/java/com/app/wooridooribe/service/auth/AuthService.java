package com.app.wooridooribe.service.auth;


import com.app.wooridooribe.controller.dto.JoinDto;
import com.app.wooridooribe.controller.dto.LoginResponseDto;
import com.app.wooridooribe.controller.dto.TokenDto;
import com.app.wooridooribe.controller.dto.TokenRequestDto;
import com.app.wooridooribe.entity.Member;

import java.util.Optional;

public interface AuthService {

    Optional<Member> getMemberById(String email);

    LoginResponseDto login(String memberId, String password);

    LoginResponseDto join(JoinDto joinDto);


    TokenDto reissue(TokenRequestDto tokenRequestDto);

}