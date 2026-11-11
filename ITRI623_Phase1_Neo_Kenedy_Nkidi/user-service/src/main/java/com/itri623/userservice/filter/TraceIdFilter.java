package com.itri623.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(TraceIdFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String traceId = request.getHeader(TRACE_ID_HEADER);

            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }

            MDC.put("traceId", traceId);
            MDC.put("serviceName", "user-service");
            MDC.put("clientIp", request.getRemoteAddr());
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());

            logger.info(
                    "Incoming request: {} {} traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    traceId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}