package com.itri623.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import org.springframework.core.Ordered;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtValidationFilter
        implements GlobalFilter, Ordered {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    JwtValidationFilter.class
            );

    @Value("${jwt.secret}")
    private String secret;

    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/register",
            "/actuator"
    };

    private Key getSigningKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request =
                exchange.getRequest();

        String path =
                request.getURI().getPath();

        String method =
                request.getMethod() != null
                        ? request.getMethod().name()
                        : "";

        /*
         * Public endpoints do not require authentication.
         */
        for (String publicPath : PUBLIC_PATHS) {

            if (path.startsWith(publicPath)) {
                return chain.filter(exchange);
            }
        }

        /*
         * All other endpoints require a Bearer token.
         */
        String authHeader =
                request.getHeaders()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        );

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            logger.warn(
                    "Missing or invalid Authorization header for method={}, path={}",
                    method,
                    path
            );

            return unauthorized(exchange);
        }

        String token =
                authHeader.substring(7);

        try {

            /*
             * Validate the JWT signature and extract claims.
             */
            Claims claims =
                    Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

            Long userId =
                    claims.get(
                            "userId",
                            Long.class
                    );

            String role =
                    claims.get(
                            "role",
                            String.class
                    );

            String email =
                    claims.getSubject();

            /*
             * Make sure the JWT contains a role.
             */
            if (role == null
                    || role.trim().isEmpty()) {

                logger.warn(
                        "JWT does not contain a role: method={}, path={}",
                        method,
                        path
                );

                return forbidden(exchange);
            }

            role = role.toUpperCase();

            logger.info(
                    "JWT validated: userId={}, role={}, method={}, path={}",
                    userId,
                    role,
                    method,
                    path
            );

            /*
             * RBAC authorization.
             *
             * The role is checked against the requested
             * endpoint before the request is forwarded.
             */
            if (!isAuthorized(role, method, path)) {

                logger.warn(
                        "RBAC denied: userId={}, role={}, method={}, path={}",
                        userId,
                        role,
                        method,
                        path
                );

                return forbidden(exchange);
            }

            /*
             * Forward trusted identity information to the
             * downstream microservice.
             */
            ServerHttpRequest mutatedRequest =
                    request.mutate()
                        .header(
                            "X-User-Id",
                            String.valueOf(userId)
                        )
                        .header(
                            "X-User-Role",
                            role
                        )
                        .header(
                            "X-User-Email",
                            email
                        )
                        .build();

            ServerWebExchange mutatedExchange =
                    exchange.mutate()
                        .request(mutatedRequest)
                        .build();

            return chain.filter(
                    mutatedExchange
            );

        } catch (Exception e) {

            logger.error(
                    "JWT validation failed for method={}, path={}: {}",
                    method,
                    path,
                    e.getMessage()
            );

            return unauthorized(exchange);
        }
    }

    /**
     * Determines whether the supplied role is allowed
     * to access the requested endpoint.
     */
    private boolean isAuthorized(
            String role,
            String method,
            String path) {

        /*
         * ADMIN has full access to protected API endpoints.
         */
        if ("ADMIN".equals(role)) {
            return true;
        }

        /*
         * USER permissions.
         */

        if ("USER".equals(role)) {

            /*
             * Users can work with tickets.
             */
            if (path.startsWith("/api/tickets")) {

                /*
                 * Users may create and view/update tickets,
                 * but may not delete tickets.
                 */
                return !"DELETE".equals(method);
            }

            /*
             * Users may view assets.
             * Asset creation, modification and deletion
             * are administrator-only operations.
             */
            if (path.startsWith("/api/assets")) {

                return "GET".equals(method);
            }

            /*
             * User-management endpoints are administrator-only.
             */
            if (path.startsWith("/api/users")) {
		if ("GET".equals(method)) {
        return true;
    }

                return false;
            }

            /*
             * Unknown protected endpoints are denied.
             */
            return false;
        }

        /*
         * Unknown roles are denied.
         */
        return false;
    }

    /**
     * Return HTTP 401 Unauthorized.
     *
     * Used when authentication is missing or invalid.
     */
    private Mono<Void> unauthorized(
            ServerWebExchange exchange) {

        ServerHttpResponse response =
                exchange.getResponse();

        response.setStatusCode(
                HttpStatus.UNAUTHORIZED
        );

        return response.setComplete();
    }

    /**
     * Return HTTP 403 Forbidden.
     *
     * Used when authentication succeeded but
     * the user's role is not allowed to perform
     * the requested operation.
     */
    private Mono<Void> forbidden(
            ServerWebExchange exchange) {

        ServerHttpResponse response =
                exchange.getResponse();

        response.setStatusCode(
                HttpStatus.FORBIDDEN
        );

        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}