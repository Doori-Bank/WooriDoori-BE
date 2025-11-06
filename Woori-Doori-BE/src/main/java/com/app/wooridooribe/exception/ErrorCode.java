package com.app.wooridooribe.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 회원 관리 - 본인 인증
    AUTH_FAIL(HttpStatus.BAD_REQUEST, "ACCOUNT-001", "인증 번호가 일치하지 않습니다."),
    TIME_OUT(HttpStatus.REQUEST_TIMEOUT, "ACCOUNT-002", "인증 시간이 초과되었습니다. 다시 인증을 시도해주세요."),
    UNAUTHORIZED_REQUEST(HttpStatus.UNAUTHORIZED, "ACCOUNT-003", "인증이 확인되지 않았습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ACCOUNT-004", "인증에 실패하였습니다."),
    
    // 회원 관리 - 회원 가입
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "ACCOUNT-005", "이미 사용 중인 이메일이 있습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "ACCOUNT-006", "비밀번호가 동일하지 않습니다."),
    REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "ACCOUNT-007", "필수 요소가 입력되지 않았습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "ACCOUNT-008", "형식에 맞지 않는 입력입니다."),
    SIGNIN_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-009", "회원가입에 실패하였습니다."),
    
    // 회원 관리 - 로그인
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-010", "아이디가 존재하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ACCOUNT-011", "비밀번호가 틀립니다."),
    
    // 회원 관리 - 아이디/비밀번호 찾기
    INVALID_USER(HttpStatus.NOT_FOUND, "ACCOUNT-012", "등록되지 않은 사용자입니다."),
    INVALID_PHONE(HttpStatus.NOT_FOUND, "ACCOUNT-013", "등록되지 않은 전화번호입니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT-014", "비활성화 된 계정입니다."),
    
    // 회원 관리 - 비밀번호 재설정
    TEMP_PASSWORD_GEN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-015", "임시 비밀번호 생성에 실패했습니다."),
    TEMP_PASSWORD_INCORRECT(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-016", "부여한 임시 비밀번호와 일치하지 않습니다."),
    UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-017", "임시 비밀번호 저장이 실패했습니다."),
    
    // 회원 관리 - 로그아웃
    DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-018", "로그아웃 처리가 불가합니다."),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "ACCOUNT-019", "이미 로그아웃 된 계정입니다."),
    
    // 소비 내역
    CARD_INVALID(HttpStatus.BAD_REQUEST, "HISTORY-001", "카드가 유효하지 않습니다."),
    HISTORY_ISNULL(HttpStatus.NOT_FOUND, "HISTORY-002", "해당 소비 내역이 존재하지 않습니다."),
    HISTORY_ISNOTYOURS(HttpStatus.FORBIDDEN, "HISTORY-003", "해당 소비 내역은 수정이 불가합니다."),
    HISTORY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-004", "수정에 실패했습니다."),
    HISTORY_INVALID_DATE(HttpStatus.BAD_REQUEST, "HISTORY-005", "유효하지 않은 연/월 값입니다."),
    // 추가
    HISTORY_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "HISTORY-006", "유효하지 않은 카테고리 값입니다."),
    HISTORY_INCLUDE_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-007", "지출 합계 포함 여부 수정에 실패했습니다."),
    HISTORY_CATEGORY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-008", "카테고리 수정에 실패했습니다."),
    HISTORY_INVALID_DUTCHPAY(HttpStatus.BAD_REQUEST, "HISTORY-009", "유효하지 않은 더치페이 인원 수입니다."),
    HISTORY_DUTCHPAY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-010", "더치페이 인원 수정에 실패했습니다."),
    HISTORY_PRICE_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-011", "금액 수정에 실패했습니다."),
    HISTORY_INVALID_PRICE(HttpStatus.BAD_REQUEST, "HISTORY-012", "유효하지 않은 금액 값입니다."),


    // 소비 일기
    DIARY_INSERT_INVALID(HttpStatus.BAD_REQUEST, "DIARY-001", "일기가 너무 깁니다."),
    DIARY_ISNOTYOURS(HttpStatus.FORBIDDEN, "DIARY-002", "해당 일기는 수정이 불가합니다."),
    DIARY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "DIARY-003", "수정에 실패했습니다."),
    DIARY_TOKEN_REVOKED(HttpStatus.FORBIDDEN, "DIARY-004", "해당 일기는 삭제에 불가합니다."),
    DIARY_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "DIARY-005", "삭제에 실패했습니다."),
    
    // 카드 - 카드 생성
    INVALID_CARD_NUMBER(HttpStatus.BAD_REQUEST, "CARD-001", "카드 번호가 옳지 않습니다."),
    CARD_EXPIRED(HttpStatus.BAD_REQUEST, "CARD-002", "카드 유효 기간이 만료되었습니다."),
    INVALID_CVC(HttpStatus.BAD_REQUEST, "CARD-003", "CVC가 일치하지 않습니다."),
    INVALID_CARD_PASSWORD(HttpStatus.UNAUTHORIZED, "CARD-004", "카드 비밀번호가 일치하지 않습니다."),
    INVALID_BIRTHDATE(HttpStatus.BAD_REQUEST, "CARD-005", "해당되는 생년월일이 없습니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "CARD-006", "별명은 10자 이하로 설정해야 합니다."),
    CARD_ELEMENT_MISSING(HttpStatus.BAD_REQUEST, "CARD-007", "카드 요소가 입력되지 않았습니다."),
    
    // 카드 - 카드 삭제/수정
    CARD_TOKEN_REVOKED(HttpStatus.GONE, "CARD-008", "이미 삭제된 카드입니다."),
    CARD_ISNULL(HttpStatus.NOT_FOUND, "CARD-009", "해당 카드는 존재하지 않습니다."),
    CARD_ISNOTYOURS(HttpStatus.FORBIDDEN, "CARD-010", "해당 카드는 수정이 불가합니다."),
    
    // 목표
    GOAL_INVALIDNUM(HttpStatus.BAD_REQUEST, "GOAL-002", "목표치가 올바르지 않습니다."),
    GOAL_ISNOTYOURS(HttpStatus.FORBIDDEN, "GOAL-003", "해당 달성도는 조회가 불가합니다."),
    GOAL_ISNULL(HttpStatus.NOT_FOUND, "GOAL-004", "해당 달성도는 존재하지 않습니다.");
    
    private final HttpStatus statusCode;
    private final String errorCode;
    private final String errorMsg;
}

