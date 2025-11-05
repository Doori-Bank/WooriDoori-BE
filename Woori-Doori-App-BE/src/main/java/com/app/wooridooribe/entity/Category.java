package com.app.wooridooribe.entity;

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
@Table(name = "tbl_category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 카테고리 고유번호

    @Column(name = "category_name", nullable = false)
    private String categoryName; // 카테고리명

    @Column(name = "category_color", nullable = false)
    private String categoryColor; // 카테고리 배경색

    // 양방향 관계 설정
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Franchise> franchises = new ArrayList<>();
}

