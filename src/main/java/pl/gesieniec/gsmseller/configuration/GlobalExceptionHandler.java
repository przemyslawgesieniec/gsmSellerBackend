package pl.gesieniec.gsmseller.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.gesieniec.gsmseller.common.EntityNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // 404 – Entity not found
    // =========================
    @ExceptionHandler(EntityNotFoundException.class)
    public Object handleEntityNotFound(
        EntityNotFoundException ex,
        HttpServletRequest request
    ) {

        if (expectsHtml(request)) {
            return "forward:/404.html";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatusException(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {

        if (expectsHtml(request)) {
            return "forward:/500.html";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getStatusCode().toString());
        body.put("message", ex.getReason() != null ? ex.getReason() : ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {

        if (expectsHtml(request)) {
            return "forward:/500.html";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // =========================
    // 500 – fallback (runtime)
    // =========================
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {

        if (expectsHtml(request)) {
            return "forward:/500.html";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("message", "Internal server error");
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @org.springframework.web.bind.annotation.ResponseBody
    public String handleIllegalState(IllegalStateException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(pl.gesieniec.gsmseller.reservation.ReservationConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @org.springframework.web.bind.annotation.ResponseBody
    public String handleReservationConflict(pl.gesieniec.gsmseller.reservation.ReservationConflictException ex) {
        return ex.getMessage();
    }


    // =========================
    // helper
    // =========================
    private boolean expectsHtml(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept == null || accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
