package com.app.wooridooribe.controller.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "가맹점 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FranchiseFileDto {

    private Long franchiseId;
    private String franName;
    private Long fileId;
    private String imageUrl;

}
