package org.intelligentsia.monitoring.infrastructure.local;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
@ConditionalOnProperty(name="app.local-configuration.enabled",havingValue="true")
public class LocalConfigurationPersistence implements ResponseBodyAdvice<Object> {
    private final LocalApplicationState state;
    @EventListener(ApplicationReadyEvent.class) public void ready() { state.save(); }
    @Override public boolean supports(MethodParameter method,Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body,MethodParameter method,MediaType contentType,
        Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response) {
        String path=request.getURI().getPath();
        boolean success=!(body instanceof ApiResponse<?> envelope) || envelope.isSuccess();
        if (response instanceof ServletServerHttpResponse servlet && servlet.getServletResponse().getStatus()>=400) success=false;
        if (success && request.getMethod()!=HttpMethod.GET &&
            (path.startsWith("/api/roles") || path.startsWith("/api/monitoring/rules") || path.startsWith("/api/settings") ||
             path.startsWith("/api/users") || path.startsWith("/api/auth") || path.startsWith("/api/scheduler") ||
             path.startsWith("/api/extraction/templates") || path.startsWith("/api/extraction/configs"))) state.save();
        return body;
    }
}
