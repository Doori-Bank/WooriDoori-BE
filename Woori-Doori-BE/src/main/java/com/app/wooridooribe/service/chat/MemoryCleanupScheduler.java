package com.app.wooridooribe.service.chat;

import groovy.util.logging.Slf4j;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryCleanupScheduler {

    private final VectorStore vectorStore;

    // 매일 새벽 3시 삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldMemory() {

        LocalDate cutoff = LocalDate.now().minusDays(30);
        String cutoffMonth = cutoff.toString().substring(0, 7);

        // month가 30일 이전이면 삭제
        List<Document> oldDocs = vectorStore.similaritySearch(
                        SearchRequest.query("dummy")   // 전체 검색하는 트릭
                                .withTopK(5000)
                ).stream()
                .filter(doc -> {
                    String month = (String) doc.getMetadata().get("month");
                    if (month == null) return false;
                    return month.compareTo(cutoffMonth) < 0;
                }).toList();

        if (!oldDocs.isEmpty()) {
            vectorStore.delete(oldDocs.stream()
                    .map(Document::getId)
                    .toList()
            );
        }
    }
}
