package com.app.wooridooribe.util;

import com.app.wooridooribe.controller.dto.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class XssUrlBlockFilter implements Filter {

    private static final Pattern XSS_PATTERN =
            Pattern.compile("(<script>|</script>|<.*?>|%3C|%3E)", Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = ((HttpServletRequest) request).getRequestURI();
        if (uri == null) {
            uri = "";
        }

        String query = ((HttpServletRequest) request).getQueryString();
        if (query == null) {
            query = "";
        }

// XSS 차단 패턴 검사
        if (uri.contains("<script>") || query.contains("<script>")) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"status\":400,\"message\":\"XSS 공격 패턴 감지됨\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }
}
