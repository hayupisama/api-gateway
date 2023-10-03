package com.mstradingcards.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

	@Autowired
	private RouteValidator validator;

	@Autowired
	private WebClient webClient;

	public AuthenticationFilter() {
		super(Config.class);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return ((exchange, chain) -> {
			if (validator.isSecured.test(exchange.getRequest())) {
				if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
					exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
					return exchange.getResponse().setComplete();
				}
				String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
				if (authHeader == null || !authHeader.startsWith("Bearer ")) {
					exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
					return exchange.getResponse().setComplete();
				}
				String token = authHeader.substring(7);
				webClient.get().uri("http://localhost:8083/api/users/")
						.headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token)).retrieve()
						.toBodilessEntity().flatMap(responseEntity -> {
							HttpStatusCode status = responseEntity.getStatusCode();
							if (!status.is2xxSuccessful()) {
								exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
								return exchange.getResponse().setComplete();
							}
							return null;
						});
			}
			return chain.filter(exchange);
		});
	}

	public static class Config {

	}
}
