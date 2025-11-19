package com.app.wooridooribe.service.s3FileService;

import com.app.wooridooribe.controller.dto.UploadedFileInfoDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3FileService {

    public UploadedFileInfoDto uploadImage(MultipartFile multipartFile) throws IOException;

    public UploadedFileInfoDto uploadImage(MultipartFile multipartFile, String folderPath) throws IOException;

    // 파일 삭제
    public boolean deleteImage(String fileName);

    // 파일 다운로드
    public byte[] downloadImage(String fileName);
}
