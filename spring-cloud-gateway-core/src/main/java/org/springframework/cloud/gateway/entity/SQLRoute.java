package org.springframework.cloud.gateway.entity;

import org.springframework.cloud.gateway.route.RouteDefinition;

public class SQLRoute {
	private String id;
	private String routePredicates;
	private String routeFilters;
	private String uri;
	private Integer order;

	public SQLRoute() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRoutePredicates() {
		return routePredicates;
	}

	public void setRoutePredicates(String routePredicates) {
		this.routePredicates = routePredicates;
	}

	public String getRouteFilters() {
		return routeFilters;
	}

	public void setRouteFilters(String routeFilters) {
		this.routeFilters = routeFilters;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}
}
