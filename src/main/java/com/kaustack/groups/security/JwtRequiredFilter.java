package com.kaustack.groups.security;

import com.kaustack.groups.exception.UnauthorizedException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 101) // Run after JwtExtractionFilter
public class JwtRequiredFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        
        if (JwtContextHolder.getToken() == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        
        chain.doFilter(request, response);
    }
}
