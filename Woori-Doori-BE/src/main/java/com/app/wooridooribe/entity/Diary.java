package com.app.wooridooribe.entity;

import com.app.wooridooribe.entity.type.EmotionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_diary")
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 소비 일기 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 유저 고유번호

    @Column(name = "diary_day")
    private LocalDate diaryDay; // 날짜

    @Column(name = "diary_emotion")
    @Enumerated(EnumType.STRING)
    private EmotionType diaryEmotion; // 소비 감정

    @Column(name = "diary_content")
    private String diaryContent; // 소비 일기 내용
}

