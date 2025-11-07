package com.app.wooridooribe.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "공통 API 응답 형식")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 제외
public class ApiResponse<T> {
    
    @Schema(description = "HTTP 상태 코드", example = "200")
    private Integer statusCode;
    
    @Schema(description = "응답 메시지 (성공 시)", example = "SUCCESS")
    private String resultMsg;
    
    @Schema(description = "에러 메시지 (실패 시)", example = "비밀번호가 틀립니다")
    private String errorResultMsg;
    
    @Schema(description = "에러 코드명 (실패 시)", example = "INVALID_CREDENTIALS")
    private String errorName;
    
    @Schema(description = "응답 데이터 (성공 시)")
    private T resultData;

    // 성공 응답 생성자
    public ApiResponse(final int statusCode, final String resultMsg) {
        this.statusCode = statusCode;
        this.resultMsg = resultMsg;
        this.resultData = null;
    }

    // 성공 응답 - resultData 포함
    public static <T> ApiResponse<T> res(final int statusCode, final String resultMsg, final T data) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .resultMsg(resultMsg)
                .resultData(data)
                .build();
    }

    // 성공 응답 - resultData 없이
    public static <T> ApiResponse<T> res(final int statusCode, final String resultMsg) {
        return res(statusCode, resultMsg, null);
    }

    // 성공 응답 간편 메서드
    public static <T> ApiResponse<T> success(T data) {
        return res(200, "SUCCESS", data);
    }

    // 에러 응답 (errorName과 ResultMsg 포함)
    public static ApiResponse<Void> error(final int statusCode, final String errorName, final String errorMessage) {
        return ApiResponse.<Void>builder()
                .statusCode(statusCode)
                .errorName(errorName)
                .errorResultMsg(errorMessage)
                .build();
    }
    
    // 에러 응답 간편 메서드 (statusCode와 메시지만)
    public static ApiResponse<Void> error(final int statusCode, final String errorMessage) {
        return ApiResponse.<Void>builder()
                .statusCode(statusCode)
                .errorResultMsg(errorMessage)
                .build();
    }
}

