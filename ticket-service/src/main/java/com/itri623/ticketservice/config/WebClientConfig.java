package com.itri623.ticketservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    private static final Logger logger =
            LoggerFactory.getLogger(WebClientConfig.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {

        return WebClient.builder()
                .filter(traceIdPropagationFilter());
    }

    private ExchangeFilterFunction traceIdPropagationFilter() {

        return ExchangeFilterFunction.ofRequestProcessor(
                clientRequest -> {

                    String traceId = MDC.get("traceId");

                    if (traceId == null || traceId.isBlank()) {
                        traceId = java.util.UUID.randomUUID().toString();
                    }

                    logger.info(
                            "Outgoing WebClient request: {} {} traceId={}",
                            clientRequest.method(),
                            clientRequest.url(),
                            traceId);

                    ClientRequest newRequest =
                            ClientRequest.from(clientRequest)
                                    .header(TRACE_ID_HEADER, traceId)
                                    .build();

                    return Mono.just(newRequest);
                }
        );
    }
}