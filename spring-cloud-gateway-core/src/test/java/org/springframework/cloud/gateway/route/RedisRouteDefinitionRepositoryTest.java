package org.springframework.cloud.gateway.route;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.gateway.route.RedisRouteDefinitionRepository.GATEWAY_ROUTES;

public class RedisRouteDefinitionRepositoryTest {

	@Autowired
	private RedisTemplate redisTemplate;

	@Test
	public void a()  {
		RouteDefinition definition = new RouteDefinition();
		definition.setId("id");
		URI uri = UriComponentsBuilder.fromHttpUrl("http://127.0.0.1:8081").build().toUri();
		// URI uri = UriComponentsBuilder.fromHttpUrl("http://baidu.com").build().toUri();
		definition.setUri(uri);

		//定义第一个断言
		PredicateDefinition predicate = new PredicateDefinition();
		predicate.setName("Path");

		Map<String, String> predicateParams = new HashMap<>(8);
		predicateParams.put("pattern", "/test");
		predicate.setArgs(predicateParams);

		//定义Filter
		FilterDefinition filter = new FilterDefinition();
		filter.setName("AddRequestHeader");
		Map<String, String> filterParams = new HashMap<>(8);
		//该_genkey_前缀是固定的，见org.springframework.cloud.gateway.support.NameUtils类
		filterParams.put("_genkey_0", "header");
		filterParams.put("_genkey_1", "addHeader");
		filter.setArgs(filterParams);

		FilterDefinition filter1 = new FilterDefinition();
		filter1.setName("AddRequestParameter");
		Map<String, String> filter1Params = new HashMap<>(8);
		filter1Params.put("_genkey_0", "param");
		filter1Params.put("_genkey_1", "addParam");
		filter1.setArgs(filter1Params);

		definition.setFilters(Arrays.asList(filter, filter1));
		definition.setPredicates(Arrays.asList(predicate));

		System.out.println("definition:" + JSON.toJSONString(definition));
		redisTemplate.opsForHash().put(GATEWAY_ROUTES, "key", JSON.toJSONString(definition));
	}
}
