package com.app.wooridooribe.entity;

import com.app.wooridooribe.entity.aduit.Period;
import com.app.wooridooribe.entity.type.JobType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_goal")
public class Goal extends Period {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 목표 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 유저 고유번호

    @Column(name = "goal_start_date")
    private LocalDate goalStartDate; // 목표시작날짜

    @Column(name = "goal_job")
    @Enumerated(EnumType.STRING)
    private JobType goalJob; // 직업

    @Column(name = "goal_income")
    private String goalIncome; // 수입

    @Column(name = "previous_goal_money")
    private Integer previousGoalMoney; // 목표금액

    @Column(name = "goal_stability_score")
    private Integer goalStabilityScore; // 소비 안정성 점수

    @Column(name = "goal_achievement_score")
    private Integer goalAchievementScore; // 목표 달성도 점수

    @Column(name = "goal_ratio_score")
    private Integer goalRatioScore; // 필수/비필수 비율 점수

    @Column(name = "goal_continuity_score")
    private Integer goalContinuityScore; // 절약 지속성 점수

    @Transient
    public Integer getGoalScore() {
        int sum = 0;

        if (goalAchievementScore != null) {
            sum += goalAchievementScore;
        }
        if (goalContinuityScore != null) {
            sum += goalContinuityScore;
        }
        if (goalRatioScore != null) {
            sum += goalRatioScore;
        }
        if (goalStabilityScore != null) {
            sum += goalStabilityScore;
        }

        return Math.round((float) sum);
    }

}

