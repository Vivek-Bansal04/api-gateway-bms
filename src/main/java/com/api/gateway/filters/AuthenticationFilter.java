package com.api.gateway.filters;

import com.api.gateway.exceptions.UnAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private RouteValidator validator;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if(validator.isSecured.test(exchange.getRequest())){
                //header contain token or not
                if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                    throw new UnAuthorizedException("Missing Authorization header");
                }
                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                authHeader = getBearerToken(authHeader);

                return validateToken(authHeader)
                        .flatMap(isValid -> {
                            if (!isValid) {
                                log.error("Unauthorized request from {}", exchange.getRequest().getRemoteAddress());
                                throw new UnAuthorizedException("Error while authenticating request");
                            }
                            // Continue to the next filter in the chain if valid
                            return chain.filter(exchange);
                        });
            }
            return chain.filter(exchange);
        });
    }

    private String getBearerToken(String token) {
        if (StringUtils.hasLength(token)) {
            return token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;
        }
        return null;
    }

    public Mono<Boolean> validateToken(String token) {
        return webClientBuilder.build()
                .post()
                .uri("http://AUTH-SERVICE/user/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(e -> {
                    log.error("Error while authenticating request", e);
                    return Mono.just(false);
                });
    }


//    public void validateToken(String token) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<?> entity = new HttpEntity<>(token, headers);
//
//        final String validateUrl = "http://AUTH-SERVICE/user/validate";
//
//        ResponseEntity<Boolean> response = restTemplate.exchange(validateUrl, HttpMethod.POST, entity, Boolean.class);
//
//        if (Boolean.FALSE.equals(response.getBody())) {
//            throw new UnAuthorizedException("Error while authenticating request");
//        }
//    }

    public static class Config{}
}
