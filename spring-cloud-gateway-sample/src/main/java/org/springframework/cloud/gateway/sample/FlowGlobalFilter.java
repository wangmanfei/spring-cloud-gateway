package org.springframework.cloud.gateway.sample;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Component
public class FlowGlobalFilter implements GlobalFilter, Ordered {
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		//获取当前请求的url
		String requestUrl = exchange.getRequest().getURI().toString();
		/*HttpHeaders headers = exchange.getRequest().getHeaders();
		Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
		Iterator<Map.Entry<String, List<String>>> iterator = entries.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, List<String>> next = iterator.next();

		}*/
		System.out.println("exchange = " + exchange + ", chain = " + chain);

		//放行请求
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return 10101;

	}
}
