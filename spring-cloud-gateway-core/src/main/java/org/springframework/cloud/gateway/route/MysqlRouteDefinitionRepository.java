/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.gateway.route;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.entity.SQLRoute;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.mapper.RoutePredicateMapper;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 */
@Component
public class MysqlRouteDefinitionRepository implements RouteDefinitionRepository , ApplicationEventPublisherAware {

	@Resource
	private RoutePredicateMapper routePredicateMapper;

	private ApplicationEventPublisher publisher;

	public void notifyChanged() {
		this.publisher.publishEvent(new RefreshRoutesEvent(this));
	}

	@Override
	public Mono<Void> save(Mono<RouteDefinition> route) {
		return null;
	}

	@Override
	public Mono<Void> delete(Mono<String> routeId) {
		return null;
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		List<RouteDefinition> routeDefinitions = new ArrayList<>();
		List<SQLRoute> sqlRoutes = routePredicateMapper.getRouteAll();
		sqlRoutes.forEach(route->{
			RouteDefinition routeDefinition = new RouteDefinition();
			routeDefinition.setId(route.getId());
			try {
				routeDefinition.setUri(new URI(route.getUri()));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			routeDefinition.setOrder(route.getOrder());
			routeDefinition.setPredicates(JSON.parseArray(route.getRoutePredicates(), PredicateDefinition.class));
			routeDefinition.setFilters(JSON.parseArray(route.getRouteFilters(), FilterDefinition.class));
			routeDefinitions.add(routeDefinition);
		});
		System.out.println("==============================自定义的mysql动态存储路由" + routeDefinitions);
		return Flux.fromIterable(routeDefinitions);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
}
