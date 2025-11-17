package com.app.wooridooribe.jwt;

import com.app.wooridooribe.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class MemberDetail implements UserDetails {

    private final Member member;

    public MemberDetail(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = member.getAuthority() != null ? member.getAuthority().toString() : "ROLE_USER";
        return Collections.singleton(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    public Long getId() {
        return member.getId();
    }

    public String getName() {
        return member.getMemberName();
    }

    @Override
    public String getUsername() {
        return member.getMemberId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}