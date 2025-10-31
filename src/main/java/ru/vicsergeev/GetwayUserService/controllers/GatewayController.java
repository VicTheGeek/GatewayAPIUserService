package ru.vicsergeev.GetwayUserService.controllers;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vicsergeev.GetwayUserService.services.ProxyService;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * Created by Victor 30.10.2025
 */

@RestController
@RequiredArgsConstructor
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);
    private final ProxyService proxyService;

    // User service routes
    @GetMapping("/users/**")
    public ResponseEntity<String> handleUserGET(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/users");
        return proxyService.forwardToUserService(path, "GET", null, request.getHeaderNames(), request);
    }

    @PostMapping(value = "/users/**", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleUserPOST(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = extractPathAfterPrefix(request, "/users");
        String payload = body != null ? new String(body, StandardCharsets.UTF_8) : null;
        log.info("Gateway: Received POST /users{}, forwarding to UserService", path);
        ResponseEntity<String> response = proxyService.forwardToUserService(path, "POST", payload, request.getHeaderNames(), request);
        log.info("Gateway: Response from UserService - Status: {}", response.getStatusCode());
        return response;
    }

    @PutMapping(value = "/users/**", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleUserPUT(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = extractPathAfterPrefix(request, "/users");
        String payload = body != null ? new String(body, StandardCharsets.UTF_8) : null;
        return proxyService.forwardToUserService(path, "PUT", payload, request.getHeaderNames(), request);
    }

    @DeleteMapping("/users/**")
    public ResponseEntity<String> handleUserDELETE(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/users");
        return proxyService.forwardToUserService(path, "DELETE", null, request.getHeaderNames(), request);
    }

    // notification service routes
    @GetMapping("/notifications/**")
    public ResponseEntity<String> handleNotificationGET(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/notifications");
        return proxyService.forwardToNotificationService(path, "GET", null, request.getHeaderNames(), request);
    }

    @PostMapping(value = "/notifications/**", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleNotificationPOST(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = extractPathAfterPrefix(request, "/notifications");
        String payload = body != null ? new String(body, StandardCharsets.UTF_8) : null;
        return proxyService.forwardToNotificationService(path, "POST", payload, request.getHeaderNames(), request);
    }

    @PutMapping(value = "/notifications/**", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> handleNotificationPUT(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = extractPathAfterPrefix(request, "/notifications");
        String payload = body != null ? new String(body, StandardCharsets.UTF_8) : null;
        return proxyService.forwardToNotificationService(path, "PUT", payload, request.getHeaderNames(), request);
    }

    @DeleteMapping("/notifications/**")
    public ResponseEntity<String> handleNotificationDELETE(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/notifications" );
        return proxyService.forwardToNotificationService(path, "DELETE", null, request.getHeaderNames(), request);
    }


    // aux method extract path after /users or /notifications
    private String extractPathAfterPrefix(HttpServletRequest request, String prefix) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());
        return path.substring(prefix.length());
    }
}
