package com.app.wooridooribe.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDto {
    @JsonProperty("id")
    private String memberId;
    private String password;
}
