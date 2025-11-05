package com.app.wooridooribe.entity;

import com.app.wooridooribe.entity.type.CardType;
import com.app.wooridooribe.entity.type.YESNO;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 카드 고유번호


    @Column(name = "card_name", nullable = false)
    private String cardName; // 카드명

    @Column(name = "card_url", length = 50)
    private String cardUrl; // 카드URL

    @Column(name="card_benef")
    private String cardBenefit;

    @Column(name="card_svc")
    @Enumerated(EnumType.STRING)
    private YESNO cardSvc;

    @Column(name="annual_fee_1")
    private String annualFee1;

    @Column(name="annual_fee_2")
    private String annualFee2;

    @Column(name ="card_type")
    @Enumerated(EnumType.STRING)
    private CardType cardType;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "file_banner_id", nullable = true)
   private File cardBanner; // 카드 배너 이미지


   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "file_id")
   private File cardImage; // 카드 이미지

    // 양방향 관계 설정
    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberCard> memberCards = new ArrayList<>();
}

