package com.app.wooridooribe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_member_card")
public class MemberCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expiry_mm_yy", nullable = false, length = 5)
    private String expiryMmYy;

    @Column(name = "card_num", nullable = false)
    private String cardNum;

    @Column(name = "card_pw", nullable = false)
    private String cardPw;

    @Column(name = "card_alias", nullable = false)
    private String cardAlias;

    @Column(name = "card_create_at", nullable = false)
    private LocalDate cardCreateAt;

    @Column(name = "card_user_name", nullable = false)
    private String cardUserName;

    @Column(name = "card_user_regist_num", nullable = false)
    private String cardUserRegistNum;

    @Column(name = "card_user_regist_back", nullable = false)
    private String cardUserRegistBack;

    @Column(name = "card_cvc", nullable = false)
    private String cardCvc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member; // 유저 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card; // 카드 고유번호

    // 양방향 관계 설정
    @OneToMany(mappedBy = "memberCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CardHistory> cardHistories = new ArrayList<>();
}
