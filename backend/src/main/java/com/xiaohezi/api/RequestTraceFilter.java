package com.xiaohezi.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestTraceFilter extends OncePerRequestFilter {
    public static final String TRACE_ID = "traceId";
    public static final String TRACE_HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader(TRACE_HEADER);
        String traceId = supplied != null && supplied.matches("[A-Za-z0-9._-]{1,64}")
            ? supplied : UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("request method={} path={} status={} durationMs={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            MDC.remove(TRACE_ID);
        }
    }
}
