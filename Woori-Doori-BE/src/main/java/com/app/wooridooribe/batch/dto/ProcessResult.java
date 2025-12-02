package com.app.wooridooribe.batch.dto;

import com.app.wooridooribe.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 배치 처리 결과를 담는 DTO
 */
@Getter
@RequiredArgsConstructor
public class ProcessResult {
    private final Member member;
    private final Status status;
    private final String reason;

    public static ProcessResult success(Member member) {
        return new ProcessResult(member, Status.SUCCESS, null);
    }

    public static ProcessResult skipped(Member member, String reason) {
        return new ProcessResult(member, Status.SKIPPED, reason);
    }

    public static ProcessResult failed(Member member, String reason) {
        return new ProcessResult(member, Status.FAILED, reason);
    }

    public enum Status {
        SUCCESS,
        SKIPPED,
        FAILED
    }
}

