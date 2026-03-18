package com.kaustack.groups.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kaustack.groups.dto.response.ErrorResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 101) // Run after JwtExtractionFilter
public class JwtRequiredFilter implements Filter {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (JwtContextHolder.getToken() == null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse body = ErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "UNAUTHORIZED",
                    "Authentication required. Please provide a valid access token.",
                    httpRequest.getRequestURI()
            );

            objectMapper.writeValue(httpResponse.getWriter(), body);
            return;
        }

        chain.doFilter(request, response);
    }
}
