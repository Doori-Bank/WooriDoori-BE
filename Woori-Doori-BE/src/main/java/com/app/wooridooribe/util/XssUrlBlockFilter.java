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

