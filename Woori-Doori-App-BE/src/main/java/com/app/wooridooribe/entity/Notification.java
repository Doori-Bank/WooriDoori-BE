package com.app.wooridooribe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 알림 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 유저 고유번호

    @Column(name = "noti_title", nullable = false)
    private String notiTitle; // 알림 제목

    @Column(name = "noti_detail", nullable = false)
    private String notiDetail; // 알림 내용

    @Column(name = "noti_date", nullable = false)
    private LocalDateTime notiDate; // 받은날짜

    @Column(name = "noti_is_checked", nullable = false)
    private Boolean notiIsChecked; // 읽음여부
}

