package com.app.dooribankbe.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private MemberAccount account;

    @Column(name = "history_date", nullable = false)
    private LocalDateTime historyDate;

    @Column(name = "history_price", nullable = false)
    private Long historyPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_status", nullable = false)
    private TransactionType historyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_category", nullable = false)
    private HistoryCategory historyCategory;

    @Column(name = "history_name", nullable = false, length = 100)
    private String historyName;

    @Column(name = "history_transfer_target")
    private String historyTransferTarget;

    @PrePersist
    void onCreate() {
        if (historyDate == null) {
            historyDate = LocalDateTime.now();
        }
    }
}

