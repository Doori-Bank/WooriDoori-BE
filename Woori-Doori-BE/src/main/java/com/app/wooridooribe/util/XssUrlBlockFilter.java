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
        String uri = req.getRequestURI();

        // XSS 문자열이 URL에 포함된 경우 즉시 차단
        if (XSS_PATTERN.matcher(uri).find()) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType("application/json;charset=UTF-8");
            ApiResponse.res(400, "XSS 공격 패턴 감지되었습니다.", null);
            return;
        }

        chain.doFilter(request, response);
        if (uri == null) {
            uri = "";
        }
        
        String query = request.getQueryString();
        if (query == null) {
            query = "";
        }
    }
}

