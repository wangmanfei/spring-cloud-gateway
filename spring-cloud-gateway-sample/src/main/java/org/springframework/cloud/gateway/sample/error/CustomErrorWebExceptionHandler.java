/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.sample.error;

import java.util.*;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Basic global {@link org.springframework.web.server.WebExceptionHandler}, rendering
 * {@link ErrorAttributes}.
 * <p>
 * More specific errors can be handled either using Spring WebFlux abstractions (e.g.
 * {@code @ExceptionHandler} with the annotation model) or by adding
 * {@link RouterFunction} to the chain.
 * <p>
 * This implementation will render error as HTML views if the client explicitly supports
 * that media type. It attempts to resolve error views using well known conventions. Will
 * search for templates and static assets under {@code '/error'} using the
 * {@link HttpStatus status code} and the {@link HttpStatus#series() status series}.
 * <p>
 * For example, an {@code HTTP 404} will search (in the specific order):
 * <ul>
 * <li>{@code '/<templates>/error/404.<ext>'}</li>
 * <li>{@code '/<static>/error/404.html'}</li>
 * <li>{@code '/<templates>/error/4xx.<ext>'}</li>
 * <li>{@code '/<static>/error/4xx.html'}</li>
 * <li>{@code '/<templates>/error/error'}</li>
 * <li>{@code '/<static>/error/error.html'}</li>
 * </ul>
 * <p>
 * If none found, a default "Whitelabel Error" HTML view will be rendered.
 * <p>
 * If the client doesn't support HTML, the error information will be rendered as a JSON
 * payload.
 *
 * @author Brian Clozel
 * @mender wm
 * @since 2.0.0
 */
public class CustomErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

	private static final Map<HttpStatus.Series, String> SERIES_VIEWS;

	private static final Log logger = LogFactory
			.getLog(DefaultErrorWebExceptionHandler.class);

	static {
		Map<HttpStatus.Series, String> views = new EnumMap<>(HttpStatus.Series.class);
		views.put(HttpStatus.Series.CLIENT_ERROR, "4xx");
		views.put(HttpStatus.Series.SERVER_ERROR, "5xx");
		SERIES_VIEWS = Collections.unmodifiableMap(views);
	}

	private final ErrorProperties errorProperties;

	/**
	 * Create a new {@code DefaultErrorWebExceptionHandler} instance.
	 * @param errorAttributes the error attributes
	 * @param resourceProperties the resources configuration properties
	 * @param errorProperties the error configuration properties
	 * @param applicationContext the current application context
	 */
	public CustomErrorWebExceptionHandler(ErrorAttributes errorAttributes,
										   ResourceProperties resourceProperties, ErrorProperties errorProperties,
										   ApplicationContext applicationContext) {
		super(errorAttributes, resourceProperties, applicationContext);
		this.errorProperties = errorProperties;
	}

	//返回路由方法基于ServerResponse的对象
	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(
			ErrorAttributes errorAttributes) {
		return route(acceptsTextHtml(), this::renderErrorView).andRoute(all(),
				this::renderErrorResponse);
	}

	/**
	 * Render the error information as an HTML view.
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	protected Mono<ServerResponse> renderErrorView(ServerRequest request) {
		boolean includeStackTrace = isIncludeStackTrace(request, MediaType.TEXT_HTML);
		Map<String, Object> error = getErrorAttributes(request, includeStackTrace);
		HttpStatus errorStatus = getHttpStatus(error);
		ServerResponse.BodyBuilder responseBody = ServerResponse.status(errorStatus)
				.contentType(MediaType.TEXT_HTML);
		return Flux
				.just("error/" + errorStatus.toString(),
						"error/" + SERIES_VIEWS.get(errorStatus.series()), "error/error")
				.flatMap((viewName) -> renderErrorView(viewName, responseBody, error))
				.switchIfEmpty(this.errorProperties.getWhitelabel().isEnabled()
						? renderDefaultErrorView(responseBody, error)
						: Mono.error(getError(request)))
				.next().doOnNext((response) -> logError(request, errorStatus));
	}

	@Override
	protected Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
		// 这里其实可以根据异常类型进行定制化逻辑
		Throwable error = super.getError(request);
		Map<String, Object> errorAttributes = new HashMap<>(8);
		errorAttributes.put("message", error.getMessage());
		errorAttributes.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
		errorAttributes.put("method", request.methodName());
		errorAttributes.put("path", request.path());
		return errorAttributes;
	}

	/**
	 * Render the error information as a JSON payload.  渲染异常Response
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		boolean includeStackTrace = isIncludeStackTrace(request, MediaType.ALL);
		Map<String, Object> error = getErrorAttributes(request, includeStackTrace);
		HttpStatus errorStatus = getHttpStatus(error);
		return ServerResponse.status(getHttpStatus(error))
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(error))
				.doOnNext((resp) -> logError(request, errorStatus));
	}

	/**
	 * Determine if the stacktrace attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the stacktrace attribute should be included
	 */
	protected boolean isIncludeStackTrace(ServerRequest request, MediaType produces) {
		ErrorProperties.IncludeStacktrace include = this.errorProperties
				.getIncludeStacktrace();
		if (include == ErrorProperties.IncludeStacktrace.ALWAYS) {
			return true;
		}
		if (include == ErrorProperties.IncludeStacktrace.ON_TRACE_PARAM) {
			return isTraceEnabled(request);
		}
		return false;
	}

	/**
	 * Get the HTTP error status information from the error map. HTTP响应状态码的封装，原来是基于异常属性的status属性进行解析的
	 * @param errorAttributes the current error information
	 * @return the error HTTP status
	 */
	protected HttpStatus getHttpStatus(Map<String, Object> errorAttributes) {
		//其实可以根据errorAttributes里面的属性定制HTTP响应码
		// TODO: 2020/2/17 0017 实用时需要改为对应的状态码
		int statusCode = (int) errorAttributes.get("status");
		return HttpStatus.valueOf(statusCode);
	}

	/**
	 * Predicate that checks whether the current request explicitly support
	 * {@code "text/html"} media type.
	 * <p>
	 * The "match-all" media type is not considered here.
	 * @return the request predicate
	 */
	protected RequestPredicate acceptsTextHtml() {
		return (serverRequest) -> {
			try {
				List<MediaType> acceptedMediaTypes = serverRequest.headers().accept();
				acceptedMediaTypes.remove(MediaType.ALL);
				MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
				return acceptedMediaTypes.stream()
						.anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
			}
			catch (InvalidMediaTypeException ex) {
				return false;
			}
		};
	}

	/**
	 * Log the original exception if handling it results in a Server Error or a Bad
	 * Request (Client Error with 400 status code) one.
	 * @param request the source request
	 * @param errorStatus the HTTP error status
	 */
	protected void logError(ServerRequest request, HttpStatus errorStatus) {
		Throwable ex = getError(request);
		log(request, ex, (errorStatus.is5xxServerError() ? logger::error : logger::warn));
	}

	private void log(ServerRequest request, Throwable ex,
					 BiConsumer<Object, Throwable> logger) {
		if (ex instanceof ResponseStatusException) {
			logger.accept(buildMessage(request, ex), null);
		}
		else {
			logger.accept(buildMessage(request, null), ex);
		}
	}

	private String buildMessage(ServerRequest request, Throwable ex) {
		StringBuilder message = new StringBuilder("Failed to handle request [");
		message.append(request.methodName());
		message.append(" ");
		message.append(request.uri());
		message.append("]");
		if (ex != null) {
			message.append(": ");
			message.append(ex.getMessage());
		}
		return message.toString();
	}

}
