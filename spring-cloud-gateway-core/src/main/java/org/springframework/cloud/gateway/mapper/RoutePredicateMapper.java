package org.springframework.cloud.gateway.mapper;


import org.springframework.cloud.gateway.entity.SQLRoute;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RoutePredicateMapper {

	List<SQLRoute> getRouteAll();
}
