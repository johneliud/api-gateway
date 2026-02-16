package io.github.johneliud.api_gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Order(-2)
@Slf4j
public class GlobalErrorHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        HttpStatus status = getHttpStatus(error);
        
        Map<String, Object> errorAttributes = Map.of(
            "success", false,
            "message", getErrorMessage(error),
            "path", exchange.getRequest().getPath().value(),
            "status", status.value()
        );
        
        log.error("Gateway error: {} - {}", exchange.getRequest().getPath(), error.getMessage());
        
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorAttributes);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    private String getErrorMessage(Throwable error) {
        if (error instanceof ResponseStatusException) {
            String reason = ((ResponseStatusException) error).getReason();
            return reason != null ? reason : "Request failed";
        }
        if (error.getMessage() != null && error.getMessage().contains("Connection refused")) {
            return "Service temporarily unavailable";
        }
        return "An error occurred processing your request";
    }

    private HttpStatus getHttpStatus(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return HttpStatus.valueOf(((ResponseStatusException) error).getStatusCode().value());
        }
        if (error.getMessage() != null && error.getMessage().contains("Connection refused")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
