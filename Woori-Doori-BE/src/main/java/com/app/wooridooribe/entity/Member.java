package com.app.wooridooribe.entity;

import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.StatusType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_member")
@ToString
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password")
    private String password;

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "birth_back")
    private String birthBack;

    @Column(name = "member_name")
    private String memberName;

    @Enumerated(EnumType.STRING)
    private StatusType status;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Authority authority;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate; // 최근 로그인 일시

    // 양방향 관계 설정
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberCard> memberCards = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Diary> diaries = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Goal> goals = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CategoryMember> categoryMembers = new ArrayList<>();
}