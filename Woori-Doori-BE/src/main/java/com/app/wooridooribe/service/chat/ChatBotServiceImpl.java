package com.app.wooridooribe.service.chat;

import com.app.wooridooribe.controller.dto.CategorySummaryDto;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.cardhistory.CardHistoryRepository;
import com.app.wooridooribe.repository.goal.GoalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatBotServiceImpl implements ChatBotService {

    private final ChatModel chatModel; // Groq용
    @SuppressWarnings("unused")
    private final EmbeddingModel embeddingModel; // 임베딩용
    private final VectorStore vectorStore; // Chroma
    private final String personaPrompt; // 페르소나 프롬프트
    private final GoalRepository goalRepository;
    private final CardHistoryRepository cardHistoryRepository;

    private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d{4})?\\s*년?\\s*(\\d{1,2})\\s*월");

    public ChatBotServiceImpl(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
            VectorStore vectorStore,
            @Value("${app.chat.persona:당신은 친절하고 도움이 되는 AI 어시스턴트입니다. 사용자의 질문에 정확하고 유용한 답변을 제공하세요.}") String personaPrompt,
            GoalRepository goalRepository,
            CardHistoryRepository cardHistoryRepository) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.personaPrompt = personaPrompt;
        this.goalRepository = goalRepository;
        this.cardHistoryRepository = cardHistoryRepository;
    }

    @Override
    public String chat(String message, Member member) {
        Long userId = member.getId();
        try {
            // 1. Goal 전체 조회 (점수 파악)
            List<Goal> goals;
            try {
                goals = goalRepository.findAllGoalsByMember(member);
                // Goal이 없어도 빈 리스트로 처리 (에러 아님)
                if (goals == null) {
                    goals = new ArrayList<>();
                }
            } catch (Exception e) {
                log.error("Goal 조회 실패: userId={}, memberId={}, error={}", userId, member.getMemberId(), e.getMessage(),
                        e);
                // Goal 조회 실패 시 빈 리스트로 처리하여 채팅은 계속 진행
                goals = new ArrayList<>();
            }

            // 2. 목표 시작일을 기준으로 한 달 범위의 카테고리 TOP 4 조회
            Goal latestGoal = extractLatestGoal(goals);
            LocalDate goalPeriodStart = latestGoal != null ? latestGoal.getGoalStartDate() : null;
            LocalDate goalPeriodEndExclusive = goalPeriodStart != null ? goalPeriodStart.plusMonths(1) : null;

            // 2-1. 최신 목표 시작일 기준 범위 외 질문 차단
            if (goalPeriodStart != null && isOutOfRangeRequest(message, goalPeriodStart)) {
                return buildOutOfRangeResponse(goalPeriodStart);
            }

            List<CategorySummaryDto> topCategories = new ArrayList<>();
            if (goalPeriodStart != null && goalPeriodEndExclusive != null) {
                try {
                    topCategories = cardHistoryRepository.findTop4CategoriesByUserIdWithinPeriod(
                            userId,
                            goalPeriodStart,
                            goalPeriodEndExclusive);
                    if (topCategories == null) {
                        topCategories = new ArrayList<>();
                    }
                } catch (Exception e) {
                    log.error("카테고리 조회 실패: userId={}, start={}, endExclusive={}, error={}",
                            userId,
                            goalPeriodStart,
                            goalPeriodEndExclusive,
                            e.getMessage(),
                            e);
                    // 카테고리 조회 실패 시 빈 리스트로 처리하여 채팅은 계속 진행
                    topCategories = new ArrayList<>();
                }
            }

            // 3. VectorStore에서 유사 문서 검색 (RAG)
            List<Document> similarDocuments;
            try {
                similarDocuments = vectorStore.similaritySearch(
                        SearchRequest.query(message)
                                .withTopK(3) // 상위 3개 문서 검색
                );
            } catch (Exception e) {
                log.warn("VectorStore 검색 실패: message={}, error={}", message, e.getMessage());
                similarDocuments = new ArrayList<>(); // RAG 실패 시 빈 리스트로 처리
            }

            // 4. 검색된 문서를 컨텍스트로 구성
            String context = similarDocuments.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining(" "));

            // 5. 사용자 데이터를 문자열로 변환
            String userData = buildUserDataString(latestGoal, topCategories, goalPeriodStart);

            // 6. 최신 Goal 점수 추출 (가장 최근 날짜의 Goal)
            Integer latestScore = latestGoal != null && latestGoal.getGoalScore() != null
                    ? latestGoal.getGoalScore()
                    : 0;

            // 7. 페르소나 프롬프트와 컨텍스트, 사용자 데이터를 포함한 SystemMessage 생성
            String systemPrompt = buildSystemPrompt(context, userData, latestScore, goalPeriodStart);
            SystemMessage systemMessage = new SystemMessage(systemPrompt);

            // 8. 사용자 메시지 생성
            UserMessage userMessage = new UserMessage(message);

            // 9. 메시지 리스트 구성
            List<Message> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(userMessage);

            // 10. Prompt 생성 및 LLM 호출
            try {
                Prompt prompt = new Prompt(messages);
                return chatModel.call(prompt).getResult().getOutput().getContent();
            } catch (Exception e) {
                log.error("LLM 호출 실패: message={}, error={}", message, e.getMessage(), e);
                throw new CustomException(ErrorCode.CHAT_LLM_ERROR);
            }
        } catch (CustomException e) {
            throw e; // CustomException은 그대로 전파
        } catch (Exception e) {
            log.error("채팅 처리 중 예상치 못한 오류 발생: userId={}, message={}, error={}", userId, message, e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "채팅 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 데이터를 문자열로 변환
     */
    private String buildUserDataString(Goal latestGoal, List<CategorySummaryDto> topCategories,
            LocalDate goalPeriodStart) {
        StringBuilder data = new StringBuilder();

        if (latestGoal != null) {
            data.append("=== 최신 목표 정보 (가장 최근 goal_start_date 기준) ===\n");
            int goalAmountManwon = latestGoal.getPreviousGoalMoney() != null
                    ? latestGoal.getPreviousGoalMoney()
                    : 0;
            long goalAmountWon = goalAmountManwon * 10_000L;
            data.append(String.format("- 목표 시작일: %s, 목표 금액: %,d만원 (%,d원), 점수: %d점\n",
                    latestGoal.getGoalStartDate(),
                    goalAmountManwon,
                    goalAmountWon,
                    latestGoal.getGoalScore() != null ? latestGoal.getGoalScore() : 0));
            data.append("\n");
        }

        // 카테고리 TOP 4 데이터
        if (!topCategories.isEmpty()) {
            if (goalPeriodStart != null) {
                YearMonth period = YearMonth.from(goalPeriodStart);
                data.append(String.format("=== %d년 %d월(목표 시작일 기준 한 달) 소비 카테고리 TOP 4 ===\n",
                        period.getYear(),
                        period.getMonthValue()));
            } else {
                data.append("=== 최근 소비 카테고리 TOP 4 ===\n");
            }
            for (int i = 0; i < topCategories.size(); i++) {
                CategorySummaryDto category = topCategories.get(i);
                String categoryName = toKoreanCategoryName(category.getCategory());
                long totalAmount = category.getTotalAmount() != null ? category.getTotalAmount() : 0L;
                long count = category.getCount() != null ? category.getCount() : 0L;
                data.append(String.format("%d. %s: %,d원 (거래 건수: %,d건)\n",
                        i + 1,
                        categoryName,
                        totalAmount,
                        count));
            }
        } else {
            data.append("=== 최근 소비 카테고리 ===\n");
            data.append("소비 내역이 없습니다.\n");
        }

        return data.toString();
    }

    private String toKoreanCategoryName(String category) {
        if (category == null)
            return "";

        return switch (category) {
            case "HOSPITAL" -> "병원";
            case "EDUCATION" -> "교육";
            case "TRAVEL" -> "여행";
            case "CONVENIENCE_STORE" -> "편의점/마트";
            case "FOOD" -> "식비";
            case "TRANSPORT" -> "교통";
            case "CAFE" -> "카페";
            case "ALCOHOL_ENTERTAINMENT" -> "술/유흥";
            case "HOUSING" -> "주거";
            case "ETC" -> "기타";
            case "TELECOM" -> "통신";
            case "ALL" -> "모든 가맹점";

            default -> category; // 매핑 안 해둔 건 그냥 원래 값
        };
    }

    /**
     * 페르소나 프롬프트와 검색된 컨텍스트, 사용자 데이터를 결합하여 SystemPrompt 생성
     */
    private String buildSystemPrompt(String context, String userData, Integer latestScore, LocalDate goalPeriodStart) {
        StringBuilder prompt = new StringBuilder();

        // 페르소나 프롬프트에 사용자 데이터 주입
        String personaWithData = personaPrompt
                .replace("{score}", String.valueOf(latestScore))
                .replace("{summary_report}", userData)
                .replace("{card_history}", userData);

        prompt.append(personaWithData);
        if (goalPeriodStart != null) {
            YearMonth period = YearMonth.from(goalPeriodStart);
            prompt.append("\n\n⚠️ 매우 중요: 오직 ")
                    .append(period.getYear())
                    .append("년 ")
                    .append(period.getMonthValue())
                    .append("월 목표 시작일을 기준으로 한 달 범위의 데이터만 답변하세요. ")
                    .append("지난달, 저번달, 9월, 10월 등 과거 기록에 대한 질문이 들어오면 '해당 기간의 데이터는 제공할 수 없습니다'라고 답변하고, ")
                    .append("오직 최신 목표 시작일(")
                    .append(period.getYear())
                    .append("년 ")
                    .append(period.getMonthValue())
                    .append("월) 기준 한 달 범위만 안내하세요.");
        } else {
            prompt.append("\n중요: 제공된 데이터 범위를 벗어나는 내용은 언급하거나 추측하지 마세요.");
        }

        prompt.append("\n\n답변 작성 시 지침:");
        prompt.append("\n1. 반드시 최신 목표 정보와 소비 카테고리 TOP4 데이터를 활용하여 구체적인 피드백과 개선 팁을 제시합니다.");
        prompt.append("\n2. 각 카테고리별로 현재 소비 상태를 평가하고, 절감 방법이나 대체 아이디어 등 실천 가능한 조언을 최소 2가지 이상 제공합니다.");
        prompt.append("\n3. 사용자가 바로 실행할 수 있도록 단계(예: 오늘 할 일, 이번 주 할 일)를 나누어 설명합니다.");
        prompt.append("\n4. 데이터가 부족한 경우에는 부족한 이유를 설명하되, 추측은 금지합니다.");
        prompt.append("\n5. 사실 전달만 필요한 항목(예: 점수, 금액, 카테고리 이름)은 짧은 문장이나 간결하게 표현합니다.");

        // 사용자 데이터 추가
        if (!userData.isEmpty()) {
            prompt.append("\n=== 사용자 소비 데이터 ===\n");
            prompt.append(userData);
        }

        // RAG 컨텍스트 추가
        if (!context.isEmpty()) {
            prompt.append("\n=== 참고 정보 ===\n");
            prompt.append(context);
        }

        return prompt.toString();
    }

    private Goal extractLatestGoal(List<Goal> goals) {
        if (goals == null || goals.isEmpty()) {
            return null;
        }

        return goals.stream()
                .filter(goal -> goal.getGoalStartDate() != null)
                .max(Comparator.comparing(Goal::getGoalStartDate))
                .orElseGet(() -> goals.stream()
                        .max(Comparator.comparing(Goal::getId, Comparator.nullsLast(Long::compareTo)))
                        .orElse(null));
    }

    private boolean isOutOfRangeRequest(String message, LocalDate goalPeriodStart) {
        if (message == null || goalPeriodStart == null) {
            return false;
        }

        String normalized = message.replaceAll("\\s+", "").toLowerCase();
        YearMonth goalPeriod = YearMonth.from(goalPeriodStart);
        int goalYear = goalPeriod.getYear();
        int goalMonth = goalPeriod.getMonthValue();

        // 지난달/저번달/전달 등 명시적 과거 표현 차단
        if (normalized.contains("지난달") || normalized.contains("저번달") || normalized.contains("전달")
                || normalized.contains("이전달") || normalized.contains("과거") || normalized.contains("이전")) {
            return true;
        }

        // "이번달", "이달", "최근달"은 허용
        if (normalized.contains("이번달") || normalized.contains("이달") || normalized.contains("최근달")
                || normalized.contains("현재")) {
            return false;
        }

        // "9월", "10월" 같은 단순 월 언급 차단 (목표 시작월이 아닌 경우)
        Pattern simpleMonthPattern = Pattern.compile("(\\d{1,2})\\s*월");
        Matcher simpleMatcher = simpleMonthPattern.matcher(message);
        while (simpleMatcher.find()) {
            try {
                int month = Integer.parseInt(simpleMatcher.group(1));
                if (month >= 1 && month <= 12 && month != goalMonth) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // "YYYY년 MM월" 패턴 체크
        Matcher matcher = MONTH_PATTERN.matcher(message);
        while (matcher.find()) {
            String yearGroup = matcher.group(1);
            String monthGroup = matcher.group(2);
            try {
                int month = Integer.parseInt(monthGroup);
                if (month < 1 || month > 12) {
                    continue;
                }

                if (yearGroup != null && !yearGroup.isBlank()) {
                    int year = Integer.parseInt(yearGroup.trim());
                    if (year != goalYear || month != goalMonth) {
                        return true;
                    }
                } else if (month != goalMonth) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }

    private String buildOutOfRangeResponse(LocalDate goalPeriodStart) {
        YearMonth period = YearMonth.from(goalPeriodStart);
        return String.format(
                "죄송하지만, 현재 제공 가능한 데이터는 최신 목표 시작일인 %d년 %d월 한 달 범위입니다. 지난달, 저번달 등 과거 기록에 대한 질문은 안내드릴 수 없습니다.",
                period.getYear(),
                period.getMonthValue());
    }
}
