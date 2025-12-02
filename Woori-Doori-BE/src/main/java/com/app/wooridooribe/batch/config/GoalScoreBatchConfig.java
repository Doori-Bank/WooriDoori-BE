package com.app.wooridooribe.batch.config;

import com.app.wooridooribe.batch.processor.GoalScoreItemProcessor;
import com.app.wooridooribe.batch.reader.MemberItemReader;
import com.app.wooridooribe.batch.writer.GoalScoreItemWriter;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GoalScoreBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final GoalScoreItemProcessor goalScoreItemProcessor;
    private final GoalScoreItemWriter goalScoreItemWriter;
    private final MemberRepository memberRepository;

    /**
     * 멀티스레드 TaskExecutor 설정
     * - corePoolSize: 기본 스레드 수
     * - maxPoolSize: 최대 스레드 수
     * - queueCapacity: 대기 큐 크기
     */
    @Bean(name = "goalScoreTaskExecutor")
    public TaskExecutor goalScoreTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 5개 스레드
        executor.setMaxPoolSize(10); // 최대 10개 스레드
        executor.setQueueCapacity(100); // 대기 큐 100개
        executor.setThreadNamePrefix("goal-score-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * MemberItemReader Bean 생성 (@StepScope 사용)
     * 각 Step 실행마다 새로운 인스턴스 생성
     */
    @Bean
    @StepScope
    public MemberItemReader memberItemReader() {
        log.info("MemberItemReader Bean 생성 - StepScope");
        MemberItemReader reader = new MemberItemReader();
        reader.setMemberRepository(memberRepository);
        return reader;
    }

    /**
     * 목표 점수 계산 Step
     * - 청크 크기: 10개씩 처리
     * - 멀티스레딩 활성화 (주석 처리하여 먼저 단일 스레드로 테스트)
     */
    @Bean
    public Step calculateGoalScoreStep() {
        log.info("calculateGoalScoreStep Bean 생성");
        
        // @StepScope Bean을 사용 - Step 실행 시마다 새로운 인스턴스 생성
        // Step 빌더에서 메서드 참조를 사용하면 Spring이 Step 실행 시점에 Bean을 생성함
        return new StepBuilder("calculateGoalScoreStep", jobRepository)
                .<Member, com.app.wooridooribe.batch.dto.ProcessResult>chunk(10, transactionManager) // 10개씩 청크 처리
                .reader(memberItemReader()) // @StepScope Bean - Step 실행 시 Spring이 자동으로 생성
                .processor(goalScoreItemProcessor)
                .writer(goalScoreItemWriter)
                // .taskExecutor(goalScoreTaskExecutor()) // 멀티스레딩 활성화 (일시적으로 비활성화)
                .build();
    }

    /**
     * 목표 점수 계산 Job
     */
    @Bean
    public Job calculateGoalScoreJob() {
        return new JobBuilder("calculateGoalScoreJob", jobRepository)
                .start(calculateGoalScoreStep())
                .build();
    }
}

