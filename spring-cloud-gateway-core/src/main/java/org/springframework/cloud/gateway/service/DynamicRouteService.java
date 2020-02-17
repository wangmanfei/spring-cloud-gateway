package org.springframework.cloud.gateway.service;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class DynamicRouteService implements ApplicationEventPublisherAware {

//	@Resource
//	private RouteDefinitionWriter routeDefinitionWriter;

	private ApplicationEventPublisher publisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher=applicationEventPublisher;
	}

	//刷新路由事件
	public void notifyChanged(){
		System.out.println("================刷新路由================");
		this.publisher.publishEvent(new RefreshRoutesEvent(this));
	}
}
