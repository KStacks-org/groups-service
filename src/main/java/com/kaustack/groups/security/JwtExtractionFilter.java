package com.kaustack.groups.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class JwtExtractionFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        
        try {
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                JwtContextHolder.setToken(token);
            }
            
            chain.doFilter(request, response);
            
        } finally {
            // Critical: prevent memory leaks in thread pools
            JwtContextHolder.clear();
        }
    }
}
