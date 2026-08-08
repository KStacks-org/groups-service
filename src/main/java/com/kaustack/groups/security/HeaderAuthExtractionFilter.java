package com.kaustack.groups.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class HeaderAuthExtractionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        try {
            String rawId = req.getHeader("X-User-Id");
            if (rawId != null) {
                AuthContext ctx = new AuthContext(
                        UUID.fromString(rawId),
                        req.getHeader("X-User-Name"),
                        req.getHeader("X-User-Email"),
                        req.getHeader("X-User-Gender")
                );
                AuthContextHolder.set(ctx);
            }

            chain.doFilter(request, response);

        } finally {
            // Critical: prevent memory leaks in thread pools
            AuthContextHolder.clear();
        }
    }
}
