package com.app.wooridooribe.controller.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFileInfoDto {

    private String fileUrl;			// 업로드한 파일 URL
    private String fileName;		// 업로드한 파일 이름

}