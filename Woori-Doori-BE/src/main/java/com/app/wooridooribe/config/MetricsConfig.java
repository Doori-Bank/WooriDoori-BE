package com.app.wooridooribe.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {
    
    private final AtomicLong asyncJobStartTime = new AtomicLong(0);
    private final AtomicLong asyncJobEndTime = new AtomicLong(0);
    
    @Bean
    public Timer httpRequestTimer(MeterRegistry registry) {
        return Timer.builder("http.server.requests.custom")
                .description("HTTP 요청 처리 시간")
                .register(registry);
    }
    
    @Bean
    public Counter httpRequestCounter(MeterRegistry registry) {
        return Counter.builder("http.server.requests.total")
                .description("HTTP 요청 총 개수")
                .register(registry);
    }
    
    @Bean
    public Counter httpSuccessCounter(MeterRegistry registry) {
        return Counter.builder("http.server.requests.success")
                .description("HTTP 성공 요청 개수")
                .tag("status", "success")
                .register(registry);
    }
    
    @Bean
    public Counter httpErrorCounter(MeterRegistry registry) {
        return Counter.builder("http.server.requests.error")
                .description("HTTP 실패 요청 개수")
                .tag("status", "error")
                .register(registry);
    }
    
    // 비동기 작업 메트릭
    @Bean
    public Timer asyncJobTimer(MeterRegistry registry) {
        return Timer.builder("async.job.duration")
                .description("비동기 작업 처리 시간")
                .register(registry);
    }
    
    @Bean
    public Counter asyncJobStartCounter(MeterRegistry registry) {
        return Counter.builder("async.job.started")
                .description("비동기 작업 시작 횟수")
                .register(registry);
    }
    
    @Bean
    public Counter asyncJobSuccessCounter(MeterRegistry registry) {
        return Counter.builder("async.job.completed")
                .description("비동기 작업 성공 횟수")
                .tag("status", "success")
                .register(registry);
    }
    
    @Bean
    public Counter asyncJobFailureCounter(MeterRegistry registry) {
        return Counter.builder("async.job.failed")
                .description("비동기 작업 실패 횟수")
                .tag("status", "failure")
                .register(registry);
    }
    
    @Bean
    public Gauge asyncJobStartTimeGauge(MeterRegistry registry) {
        return Gauge.builder("async.job.start.time", asyncJobStartTime, AtomicLong::get)
                .description("비동기 작업 시작 시간 (Unix timestamp)")
                .register(registry);
    }
    
    @Bean
    public Gauge asyncJobEndTimeGauge(MeterRegistry registry) {
        return Gauge.builder("async.job.end.time", asyncJobEndTime, AtomicLong::get)
                .description("비동기 작업 완료 시간 (Unix timestamp)")
                .register(registry);
    }
    
    public AtomicLong getAsyncJobStartTime() {
        return asyncJobStartTime;
    }
    
    public AtomicLong getAsyncJobEndTime() {
        return asyncJobEndTime;
    }
}

