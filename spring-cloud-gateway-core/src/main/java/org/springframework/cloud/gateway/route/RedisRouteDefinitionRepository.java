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
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 */
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {

	//用于存放在redis中网关的key
	public static final String GATEWAY_ROUTES = "GETEWAYROUTES";

	@Resource
	private RedisTemplate<String,String> redisTemplate;

	@Override
	public Mono<Void> save(Mono<RouteDefinition> route) {
		return route.flatMap( r -> {
			redisTemplate.opsForList().leftPush(GATEWAY_ROUTES,JSON.toJSONString(r));
			return Mono.empty();
		});
	}

	@Override
	public Mono<Void> delete(Mono<String> routeId) {
		System.out.println("==========删除===============");
		if (routeId == null){
			return Mono.defer(() -> Mono.error(new NotFoundException("RouteDefinition not found: "+routeId)));
		}
		List<String> redisList = redisTemplate.opsForList().range(GATEWAY_ROUTES, 0, redisTemplate.opsForList().size(GATEWAY_ROUTES)-1);
		List<String> newRedisList=new ArrayList<>();
		return routeId.flatMap(id->{
			redisList.forEach(routeDefinition->{
				RouteDefinition routeDefinition1 = JSON.parseObject(routeDefinition.toString(), RouteDefinition.class);
				if (!id.equals(routeDefinition1.getId())){
					newRedisList.add(routeDefinition);
				}
			});
			//如果在redis中有此路由则删除原有的所有，添加新的集合
			if (redisList.size()<newRedisList.size()){
				redisTemplate.delete(GATEWAY_ROUTES);
				newRedisList.forEach(newRoute->{
					redisTemplate.opsForList().leftPush(GATEWAY_ROUTES,JSON.toJSONString(newRoute));
				});
				return Mono.empty();
			}
			return Mono.defer(() -> Mono.error(new NotFoundException("RouteDefinition not found: "+routeId)));
		});
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		List<RouteDefinition> routeDefinitions = new ArrayList<>();
		List<String> redisList = redisTemplate.opsForList().range(GATEWAY_ROUTES, 0, redisTemplate.opsForList().size(GATEWAY_ROUTES)-1);
		redisList.forEach(routeDefinition -> routeDefinitions.add(JSON.parseObject(routeDefinition.toString(), RouteDefinition.class)));
		System.out.println("==============================自定义的redis动态存储路由"+routeDefinitions);
		return Flux.fromIterable(routeDefinitions);
	}
}
