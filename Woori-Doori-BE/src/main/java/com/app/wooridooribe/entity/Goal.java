package com.app.wooridooribe.entity;

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
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 목표 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 유저 고유번호

    @Column(name = "goal_start_date")
    private LocalDate goalStartDate; // 목표시작날짜

    @Column(name = "previous_goal_money")
    private Integer previousGoalMoney; // 목표금액 (ERD에서는 DATE 타입으로 되어있지만 실제로는 금액)

    @Column(name = "goal_score")
    private Integer goalScore; // 받은 점수

    @Column(name = "goal_comment")
    private String goalComment; // 둘리 코멘트

    @Column(name = "goal_job")
    @Enumerated(EnumType.STRING)
    private JobType goalJob; // 직업

    @Column(name = "goal_income")
    private String goalIncome; // 수입
}

