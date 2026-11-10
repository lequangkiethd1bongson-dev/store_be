package vn.team4.store.supplier.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class SupplierRequestLoggingInterceptor implements HandlerInterceptor {
    public static final String STARTED_AT_ATTRIBUTE =
            SupplierRequestLoggingInterceptor.class.getName() + ".startedAt";

    private static final Logger log =
            LoggerFactory.getLogger(SupplierRequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        Instant startedAt = Instant.now();
        request.setAttribute(STARTED_AT_ATTRIBUTE, startedAt);
        log.info("Supplier request started at={} method={} uri={} query={}",
                startedAt, request.getMethod(), request.getRequestURI(), querySummary(request));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        Object value = request.getAttribute(STARTED_AT_ATTRIBUTE);
        long durationMs = value instanceof Instant startedAt
                ? Duration.between(startedAt, Instant.now()).toMillis()
                : -1;
        if (exception == null) {
            log.info("Supplier request completed method={} uri={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            return;
        }

        log.error("Supplier request failed method={} uri={} status={} durationMs={} "
                        + "exceptionClass={} message={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs,
                exception.getClass().getName(), exception.getMessage(), exception);
    }

    public static String querySummary(HttpServletRequest request) {
        Map<String, String> summary = new TreeMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if ("keyword".equalsIgnoreCase(name)) {
                String keyword = values.length == 0 ? "" : values[0];
                summary.put(name, "<redacted,length=" + keyword.length() + ">");
            } else if (List.of("includeHidden", "isActive", "active", "status", "page", "size")
                    .contains(name)) {
                summary.put(name, values.length == 0 ? "" : String.join(",", values));
            } else {
                summary.put(name, "<redacted>");
            }
        });
        return summary.toString();
    }
}
