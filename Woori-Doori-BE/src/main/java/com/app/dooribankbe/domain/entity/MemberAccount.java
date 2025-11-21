package com.app.dooribankbe.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_member_account")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK는 수동으로 설정 (동기화용)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private DB2Member DB2Member;

    @Column(name = "account_num", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "account_password", nullable = false, length = 4)
    private String accountPassword;

    @Column(name = "account_create_at", nullable = false)
    private LocalDate accountCreateAt;

    @Column(nullable = false)
    private Long balance;

    @PrePersist
    void onCreate() {
        if (accountCreateAt == null) {
            accountCreateAt = LocalDate.now();
        }
        if (balance == null) {
            balance = 0L;
        }
    }
}

