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
@Table(name = "tbl_franchise")
public class Franchise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 가맹점 ID

    @Enumerated(EnumType.STRING)
    @Column(name="category")
    private CategoryType category; // 카테고리 ENUM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file; // 파일 ID

    @Column(name = "fran_name")
    private String franName; // 가맹점명
}

