package com.app.wooridooribe.batch.writer;

import com.app.wooridooribe.batch.dto.ProcessResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 청크 단위로 처리된 결과를 기록하는 ItemWriter
 * 실제 DB 업데이트는 Processor에서 이미 완료되었으므로 여기서는 통계만 기록
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoalScoreItemWriter implements ItemWriter<ProcessResult> {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);

    @Override
    public void write(Chunk<? extends ProcessResult> chunk) throws Exception {
        List<? extends ProcessResult> results = chunk.getItems();
        
        int success = 0;
        int skipped = 0;
        int failed = 0;
        
        // 각 결과의 상태별로 카운트
        for (ProcessResult result : results) {
            switch (result.getStatus()) {
                case SUCCESS:
                    success++;
                    successCount.incrementAndGet();
                    break;
                case SKIPPED:
                    skipped++;
                    skippedCount.incrementAndGet();
                    break;
                case FAILED:
                    failed++;
                    failedCount.incrementAndGet();
                    break;
            }
        }
        
        // 메트릭 기록
        if (success > 0) {
            Counter.builder("batch.goal_score.success")
                    .register(meterRegistry)
                    .increment(success);
        }
        if (skipped > 0) {
            Counter.builder("batch.goal_score.skipped")
                    .register(meterRegistry)
                    .increment(skipped);
        }
        if (failed > 0) {
            Counter.builder("batch.goal_score.failed")
                    .register(meterRegistry)
                    .increment(failed);
        }
        
        log.debug("청크 처리 완료 - 성공: {}, 스킵: {}, 실패: {}", success, skipped, failed);
    }

    /**
     * 전체 성공 카운트 반환 (Job 완료 후 통계용)
     */
    public int getSuccessCount() {
        return successCount.get();
    }

    /**
     * 전체 스킵 카운트 반환
     */
    public int getSkippedCount() {
        return skippedCount.get();
    }

    /**
     * 전체 실패 카운트 반환
     */
    public int getFailedCount() {
        return failedCount.get();
    }

    /**
     * 카운터 리셋 (새 Job 시작 시)
     */
    public void resetCounters() {
        successCount.set(0);
        skippedCount.set(0);
        failedCount.set(0);
    }
}

