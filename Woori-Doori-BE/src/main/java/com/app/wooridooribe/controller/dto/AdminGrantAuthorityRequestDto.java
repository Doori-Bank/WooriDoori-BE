package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.Authority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 권한 변경 요청 DTO")
public class AdminGrantAuthorityRequestDto {

    @NotBlank
    @Schema(description = "회원 ID (이메일)", example = "test@example.com", required = true)
    private String memberId;

    @NotNull
    @Schema(description = "설정할 권한", example = "ADMIN", required = true, allowableValues = {"USER", "ADMIN"})
    private Authority authority;
}

