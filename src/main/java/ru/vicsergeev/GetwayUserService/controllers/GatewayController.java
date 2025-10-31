package ru.vicsergeev.GetwayUserService.controllers;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vicsergeev.GetwayUserService.services.ProxyService;

import lombok.RequiredArgsConstructor;

/**
 * Created by Victor 30.10.2025
 */

@RestController
@RequiredArgsConstructor
public class GatewayController {

    private final ProxyService proxyService;

    // User service routes
    @GetMapping("/users/**")
    public ResponseEntity<String> handleUserGET(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/users");
        return proxyService.forwardToUserService(path, "GET", null, request.getHeaderNames(), request);
    }

    @PostMapping("/users/**")
    public ResponseEntity<String> handleUserPOST(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/users");
        return proxyService.forwardToUserService(path, "POST", null, request.getHeaderNames(), request);
    }

    @PutMapping("/users/**")
    public ResponseEntity<String> handleUserPUT(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/users");
        return proxyService.forwardToUserService(path, "PUT", null, request.getHeaderNames(), request);
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

    @PostMapping("/notifications/**")
    public ResponseEntity<String> handleNotificationPOST(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/notifications");
        return proxyService.forwardToNotificationService(path, "POST", null, request.getHeaderNames(), request);
    }

    @PutMapping("/notifications/**")
    public ResponseEntity<String> handleNotificationPUT(HttpServletRequest request) {
        String path = extractPathAfterPrefix(request, "/notifications");
        return proxyService.forwardToNotificationService(path, "PUT", null, request.getHeaderNames(), request);
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
