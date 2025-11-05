package com.app.wooridooribe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_card_history")
public class CardHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 결제내역 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_card_id", nullable = true)
    private MemberCard memberCard; // 유저 카드 데이터 번호

    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate; // 결제일자

    @Column(name = "history_name", nullable = false)
    private String historyName; // 결제 가맹점명

    @Column(name = "history_price", nullable = false)
    private Integer historyPrice; // 결제금액

    @Column(name = "history_status", nullable = false)
    private String historyStatus; // 결제상태 (승인/거절)

    @Column(name = "history_category", nullable = false)
    private String historyCategory; // 카테고리

    @Column(name = "history_includeTotal", nullable = false)
    private String historyIncludeTotal; // 총지출금액 포함여부

    @Column(name = "history_dutchpay", nullable = false)
    @Builder.Default
    private Integer historyDutchpay = 1; // 더치페이 참여 인원수
}

