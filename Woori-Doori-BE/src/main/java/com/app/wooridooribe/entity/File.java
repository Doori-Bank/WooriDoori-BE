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
@Table(name = "tbl_file")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 파일 번호

    @Column(nullable = false, unique = true)
    private String uuid; // 고유문자

    @Column(name = "file_origin_name", nullable = false)
    private String fileOriginName; // 파일원본이름

    @Column(name = "file_path", nullable = false)
    private String filePath; // 파일경로

    @Column(name = "file_type", nullable = false)
    private String fileType; // 파일 타입

    // 양방향 관계 설정
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Franchise> franchises = new ArrayList<>();
}

