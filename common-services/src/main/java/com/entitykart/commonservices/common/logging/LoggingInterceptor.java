package com.entitykart.commonservices.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP request/response logging interceptor (originally from common-service).
 * Logs method, URI, status code, and duration for every inbound request
 * handled by the MVC dispatcher (notification & export REST controllers).
 *
 * Note: Spring Cloud Gateway uses a separate reactive pipeline; this
 * interceptor covers only the traditional Spring MVC REST endpoints
 * (e.g. /api/admin/notifications, /api/admin/export).
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        LOGGER.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        long startTime = (long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = System.currentTimeMillis() - startTime;

        LOGGER.info(
                "Completed request: {} {} status={} duration={}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);

        if (exception != null) {
            LOGGER.error("Request failed: {} {}", request.getMethod(), request.getRequestURI(), exception);
        }
    }
}
