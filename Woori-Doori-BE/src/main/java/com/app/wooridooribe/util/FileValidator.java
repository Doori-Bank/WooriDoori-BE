package com.app.wooridooribe.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FileValidator {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    public static void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("파일이 없습니다.");
        }

        // 1) 확장자 체크
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new RuntimeException("올바르지 않은 파일 형식입니다.");
        }

        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("허용되지 않은 확장자입니다: " + ext);
        }

        // 2) MIME 타입 체크
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new RuntimeException("허용되지 않은 MIME 타입입니다: " + mimeType);
        }

        // 3) Magic Number 검사
        try {
            byte[] header = new byte[12];
            int bytesRead = file.getInputStream().read(header);

            if (bytesRead < 12) {
                throw new RuntimeException("파일이 너무 작아 유효한 이미지가 아닙니다.");
            }

            // JPG
            if (ext.equals("jpg") || ext.equals("jpeg")) {
                if (header[0] != (byte) 0xFF || header[1] != (byte) 0xD8) {
                    throw new RuntimeException("JPG 파일 형식이 올바르지 않습니다.");
                }
            }

            // PNG
            if (ext.equals("png")) {
                if (header[0] != (byte) 0x89 || header[1] != (byte) 0x50) {
                    throw new RuntimeException("PNG 파일 형식이 올바르지 않습니다.");
                }
            }

            // WEBP
            if (ext.equals("webp")) {
                // WEBP는 "RIFF" "WEBP" 구조이므로 앞 4 ~ 8 바이트 검사
                if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F') {
                    throw new RuntimeException("WEBP 파일 형식이 올바르지 않습니다.");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("파일 검사 중 오류가 발생했습니다.");
        }
    }
}
