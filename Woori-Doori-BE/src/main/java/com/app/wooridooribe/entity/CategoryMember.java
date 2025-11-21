package com.app.wooridooribe.entity;

import com.app.wooridooribe.entity.type.CategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_category_member")
public class CategoryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 카테고리-멤버 중간테이블

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 유저 고유번호

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private CategoryType categoryType; // 카테고리 (ENUM)
}

