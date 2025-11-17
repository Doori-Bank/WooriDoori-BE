package com.app.wooridooribe.service.chat;

import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.QCardHistory;
import com.app.wooridooribe.entity.QMemberCard;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.goal.GoalRepository;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatBotServiceImpl implements ChatBotService {

    private final ChatModel chatModel;
    @SuppressWarnings("unused")
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final GoalRepository goalRepository;
    private final JPAQueryFactory queryFactory;

    // 장기 기억
    private final String personaPrompt;    // 두리 페르소나
    private final String longTermRules;    // 점수 계산 방식 등

    // 단기 TTL (30일)
    private static final long SHORT_TERM_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000L;

    private static final Pattern MONTH_PATTERN =
            Pattern.compile("(\\d{4})?\\s*년?\\s*(\\d{1,2})\\s*월");

    public ChatBotServiceImpl(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
            VectorStore vectorStore,
            GoalRepository goalRepository,
            JPAQueryFactory queryFactory,
            @Value("${app.chat.persona}") Resource personaResource,
            @Value("${app.chat.rules}") Resource rulesResource
    ) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.goalRepository = goalRepository;
        this.queryFactory = queryFactory;

        try {
            this.personaPrompt = new String(personaResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            this.longTermRules = new String(rulesResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("프롬프트 파일 로딩 실패", e);
        }

        initLongTermMemory();
    }

    private void initLongTermMemory() {
        // 장기 기억 중복 저장 방지
        List<Document> existing = vectorStore.similaritySearch(SearchRequest.query("persona").withTopK(2));
        if (!existing.isEmpty()) return;

        try {
            List<Document> docs = new ArrayList<>();
            Document personaDoc = new Document(
                    personaPrompt,
                    Map.of("memoryType", "LONG", "contentType", "PERSONA")
            );

            Document ruleDoc = new Document(
                    longTermRules,
                    Map.of("memoryType", "LONG", "contentType", "RULES")
            );

            docs.add(personaDoc);
            docs.add(ruleDoc);
            vectorStore.add(docs);

            log.info("⭐ [LONG MEMORY SAVED] persona & rules 저장 완료");
            log.info("⭐ PERSONA metadata=" + personaDoc.getMetadata());
            log.info("⭐ RULES metadata=" + ruleDoc.getMetadata());
        } catch (Exception e) {
            log.warn("장기 기억 저장 실패: {}", e.getMessage());
        }
    }


    @Override
    public String chat(String message, Member member) {

        try {
            // 최신 Goal 조회
            List<Goal> goals = Optional.ofNullable(
                    goalRepository.findAllGoalsByMember(member.getId())
            ).orElse(new ArrayList<>());

            Goal latestGoal = extractLatestGoal(goals);
            LocalDate goalPeriodStart = (latestGoal != null ? latestGoal.getGoalStartDate() : null);

            // 과거 질문 차단
            if (goalPeriodStart != null && isOutOfRange(message, goalPeriodStart)) {
                return outOfRangeResponse(goalPeriodStart);
            }

            int latestScore = (latestGoal != null && latestGoal.getGoalScore() != null)
                    ? latestGoal.getGoalScore()
                    : 0;

            List<Document> ragDocs = searchShortTermMemory(message, member);
            String ragContext = ragDocs.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n"));


            // 이번 달 소비 요약 (QueryDSL)
            String monthlyContext = "";
            if (goalPeriodStart != null) {
                YearMonth ym = YearMonth.from(goalPeriodStart);
                MonthlySummary summary = loadMonthlySummary(member.getId(), ym);

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("### %d년 %d월 소비 요약\n",
                        ym.getYear(), ym.getMonthValue()));

                sb.append(String.format("- 총 소비 금액: %,d원\n", summary.totalAmount));

                // 카테고리 TOP5
                sb.append("\n### 카테고리별 소비 TOP5\n");
                summary.top5.forEach(e -> sb.append(String.format(
                        "- %s: %,d원 (%d건)\n",
                        toKor(String.valueOf(e.category)),
                        e.total,
                        e.count
                )));

                // 상세 리스트
                sb.append("\n### 상세 소비 내역 (일부)\n");
                summary.histories.stream()
                        .limit(20)
                        .forEach(h -> sb.append(String.format(
                                "- [%s] %s: %,d원 (%s)\n",
                                h.getHistoryDate(),
                                h.getHistoryName(),
                                h.getHistoryPrice(),
                                toKor(String.valueOf(h.getHistoryCategory()))
                        )));

                monthlyContext = sb.toString();

                saveShortTermMemory(member, ym, latestScore, monthlyContext);
            }

            String finalContext =
                    (ragContext.isBlank() ? "" : ragContext + "\n\n") + monthlyContext;

            // System Prompt 생성
            String systemPrompt = systemPrompt(
                    personaPrompt, longTermRules,
                    latestGoal, latestScore,
                    finalContext
            );

            // LLM 호출
            List<Message> messages = List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(message)
            );

            String promptString = systemPrompt + "\n\n" + message;
            int tokens = countTokens(promptString);
            System.out.println("🔥 TOTAL TOKENS = " + tokens);

            return chatModel.call(new Prompt(messages))
                    .getResult()
                    .getOutput()
                    .getContent();

        } catch (Exception e) {
            log.error("Chat 처리 중 오류", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "챗봇 오류");
        }
    }

    /**
     *  단기 기억(이번 달 소비 요약 + 점수)을 VectorStore에 저장
     */
    private void saveShortTermMemory(Member member, YearMonth ym, int score, String monthlyContext) {
        if (monthlyContext == null || monthlyContext.isBlank()) {
            return;
        }

        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("memoryType", "SHORT");
            meta.put("memberId", member.getId());
            meta.put("year", ym.getYear());
            meta.put("month", ym.getMonthValue());
            meta.put("score", score);
            meta.put("createdAt", System.currentTimeMillis()); // TTL 계산용
            meta.put("ttlDays", 30);

            Document doc = new Document(monthlyContext, meta);
            vectorStore.add(List.of(doc));

            log.info("⭐ [SHORT MEMORY SAVED] {}", meta);
        } catch (Exception e) {
            log.warn("단기 기억 저장 실패: {}", e.getMessage());
        }
    }

    /**
     *  단기 기억 검색: 같은 유저 + TTL 30일 이내 + SHORT 타입만
     */
    private List<Document> searchShortTermMemory(String query, Member member) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.query(query).withTopK(2)
            );

            long now = System.currentTimeMillis();

            return docs.stream()
                    // memoryType == SHORT
                    .filter(d -> "SHORT".equals(d.getMetadata().get("memoryType")))
                    // 같은 memberId
                    .filter(d -> {
                        Object mid = d.getMetadata().get("memberId");
                        if (mid == null) return false;
                        try {
                            return Long.parseLong(mid.toString()) == member.getId();
                        } catch (NumberFormatException ex) {
                            return false;
                        }
                    })
                    // TTL 30일
                    .filter(d -> {
                        Object created = d.getMetadata().get("createdAt");
                        if (created == null) return false;
                        try {
                            long c = Long.parseLong(created.toString());
                            return now - c <= SHORT_TERM_TTL_MILLIS;
                        } catch (NumberFormatException ex) {
                            return false;
                        }
                    })
                    .toList();

        } catch (Exception e) {
            log.error("단기 기억 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     *  QueryDSL로 한 달 소비 내역 + 총합 + 카테고리별 합계(TOP5) 조회
     */
    private MonthlySummary loadMonthlySummary(Long memberId, YearMonth ym) {

        QCardHistory h = QCardHistory.cardHistory;
        QMemberCard mc = QMemberCard.memberCard;

        int year = ym.getYear();
        int month = ym.getMonthValue();

        // 상세 리스트 (include_total='Y')
        List<CardHistory> histories = queryFactory.selectFrom(h)
                .join(h.memberCard, mc)
                .where(
                        mc.member.id.eq(memberId),
                        h.historyDate.year().eq(year),
                        h.historyDate.month().eq(month),
                        h.historyStatus.eq(StatusType.ABLE),
                        h.historyIncludeTotal.eq("Y")
                )
                .orderBy(h.historyDate.asc())
                .fetch();

        // 총합
        Integer total = queryFactory.select(h.historyPrice.sum())
                .from(h)
                .join(h.memberCard, mc)
                .where(
                        mc.member.id.eq(memberId),
                        h.historyDate.year().eq(year),
                        h.historyDate.month().eq(month),
                        h.historyStatus.eq(StatusType.ABLE),
                        h.historyIncludeTotal.eq("Y")
                )
                .fetchOne();

        int totalAmount = (total != null ? total : 0);

        // 카테고리별 합계 + 카운트
        Map<CategoryType, Long> categorySum = histories.stream()
                .collect(Collectors.groupingBy(
                        CardHistory::getHistoryCategory,
                        Collectors.summingLong(CardHistory::getHistoryPrice)
                ));

        Map<CategoryType, Long> categoryCount = histories.stream()
                .collect(Collectors.groupingBy(
                        CardHistory::getHistoryCategory,
                        Collectors.counting()
                ));

        // TOP5 추출
        List<CategoryStat> top5 = categorySum.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new CategoryStat(
                        e.getKey(),
                        e.getValue(),
                        categoryCount.getOrDefault(e.getKey(), 0L)
                ))
                .toList();

        return new MonthlySummary(totalAmount, histories, top5);
    }

    private record CategoryStat(CategoryType category, long total, long count) {}
    private record MonthlySummary(int totalAmount, List<CardHistory> histories, List<CategoryStat> top5) {}


    /**
     * 최신 Goal 선택 (가장 최근 goalStartDate 기준)
     */
    private Goal extractLatestGoal(List<Goal> goals) {
        if (goals == null || goals.isEmpty()) return null;

        return goals.stream()
                .filter(g -> g.getGoalStartDate() != null)
                .max(Comparator.comparing(Goal::getGoalStartDate))
                .orElse(null);
    }

    /**
     *  System Prompt 생성
     */
    private String systemPrompt(
            String persona,
            String rules,
            Goal latestGoal,
            Integer score,
            String context
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("### 두리 페르소나\n").append(persona).append("\n\n");

        sb.append("### 장기 규칙 / 점수 계산 방식\n")
                .append(rules).append("\n\n");

        // 최신 Goal 정보
        sb.append("### 사용자 점수 정보\n");
        if (latestGoal != null) {
            sb.append(String.format(
                    "- 최신 점수: %d점\n- 목표 금액: %d만원\n- 목표 기간: %s ~ %s\n",
                    score,
                    latestGoal.getPreviousGoalMoney(),
                    latestGoal.getGoalStartDate(),
                    latestGoal.getGoalStartDate().plusMonths(1)
            ));
        } else {
            sb.append("- 목표 없음\n");
        }

        // 최근 한 달 소비 데이터
        sb.append("\n### 최근 한 달 소비 데이터\n")
                .append((context == null || context.isBlank())
                        ? "소비 데이터 없음\n"
                        : context + "\n");

        sb.append("\n### 답변 지침\n")
                .append("- 공식/수식은 절대 사용자에게 공개하지 않습니다.\n")
                .append("- 카테고리별 점수를 새로 만들지 말고 서술형으로만 설명합니다.\n")
                .append("- 불필요한 메타멘트 금지.\n")
                .append("- 2~3문장으로 간결하게.\n")
                .append("- 과거 달 요청은 제공 불가.\n");

        return sb.toString();
    }

    private boolean isOutOfRange(String message, LocalDate goalPeriodStart) {
        if (message == null) return false;

        String normalized = message.replaceAll("\\s+", "").toLowerCase();
        YearMonth ym = YearMonth.from(goalPeriodStart);
        int goalMonth = ym.getMonthValue();
        int goalYear = ym.getYear();

        // 명시적 과거 표현
        if (normalized.contains("지난달")
                || normalized.contains("저번달")
                || normalized.contains("전달")
                || normalized.contains("이전달")
                || normalized.contains("이전")) {
            return true;
        }

        if (normalized.contains("이번달") || normalized.contains("이달")) return false;

        // 숫자 월
        Matcher mm = Pattern.compile("(\\d{1,2})월").matcher(message);
        while (mm.find()) {
            if (Integer.parseInt(mm.group(1)) != goalMonth) return true;
        }

        // YYYY년 MM월
        Matcher m = MONTH_PATTERN.matcher(message);
        while (m.find()) {
            String y = m.group(1);
            int month = Integer.parseInt(m.group(2));
            if (y != null && !y.equals(String.valueOf(goalYear))) return true;
            if (month != goalMonth) return true;
        }

        return false;
    }

    private String outOfRangeResponse(LocalDate start) {
        YearMonth p = YearMonth.from(start);
        return String.format(
                "죄송해요! %d년 %d월의 한 달 범위만 알려줄 수 있어요.",
                p.getYear(), p.getMonthValue()
        );
    }

    private String toKor(String category) {
        if (category == null) return "";
        return switch (category) {
            case "HOSPITAL" -> "병원";
            case "EDUCATION" -> "교육";
            case "TRAVEL" -> "여행";
            case "CONVENIENCE_STORE" -> "편의점/마트";
            case "FOOD" -> "식비";
            case "TRANSPORTATION" -> "교통";
            case "CAFE" -> "카페";
            case "ALCOHOL_ENTERTAINMENT" -> "술/유흥";
            case "HOUSING" -> "주거";
            case "ETC" -> "기타";
            case "TELECOM" -> "통신";
            default -> category;
        };
    }

    private int countTokens(String text) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);
        return enc.countTokens(text);
    }
}
