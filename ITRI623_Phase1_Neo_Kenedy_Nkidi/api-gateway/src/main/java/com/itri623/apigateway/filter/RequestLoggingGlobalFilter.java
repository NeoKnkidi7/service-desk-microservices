package com.itri623.apigateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingGlobalFilter implements GlobalFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String incomingTraceId =
                exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);

        final String traceId;

        if (incomingTraceId == null || incomingTraceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        } else {
            traceId = incomingTraceId;
        }

        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header(TRACE_ID_HEADER, traceId)
                        .build();

        ServerWebExchange mutatedExchange =
                exchange.mutate()
                        .request(mutatedRequest)
                        .build();

        MDC.put("traceId", traceId);
        MDC.put("serviceName", "api-gateway");
        MDC.put("method", exchange.getRequest().getMethod().name());
        MDC.put("path", exchange.getRequest().getURI().getPath());
        MDC.put("clientIp",
                exchange.getRequest()
                        .getRemoteAddress() != null
                        ? exchange.getRequest()
                                .getRemoteAddress()
                                .getAddress()
                                .getHostAddress()
                        : "unknown");

        logger.info(
                "Incoming request: {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath());

        return chain.filter(mutatedExchange)
                .doOnSuccess(result -> {
                    MDC.put("traceId", traceId);
                    MDC.put("serviceName", "api-gateway");

                    logger.info(
                            "Completed request: {} {} status={}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath(),
                            exchange.getResponse().getStatusCode());

                    MDC.clear();
                })
                .doOnError(error -> {
                    MDC.put("traceId", traceId);
                    MDC.put("serviceName", "api-gateway");

                    logger.error(
                            "Request failed: {} {} error={}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath(),
                            error.getMessage());

                    MDC.clear();
                })
                .then(Mono.fromRunnable(MDC::clear));
    }
}