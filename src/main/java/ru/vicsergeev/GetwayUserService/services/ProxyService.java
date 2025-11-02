package ru.vicsergeev.GetwayUserService.services;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import ru.vicsergeev.GetwayUserService.config.ServicesProperties;

import java.util.Enumeration;

/**
 * Created by Victor 31.10.2025
 */

@Service
@RequiredArgsConstructor
public class ProxyService {
    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);
    private final ServicesProperties services;
    private final ServiceDiscoveryManager serviceDiscovery;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = buildRestTemplate();
    }

    private RestTemplate buildRestTemplate() {
        // timeout from services config
        int connectTimeout = services.getUser().getConnectTimeout();
        int responseTimeout = services.getUser().getResponseTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create().build())
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackUserService")
    public ResponseEntity<String> forwardToUserService(String path, String method, String body,
                                                       Enumeration<String> headerNames, HttpServletRequest request) {
        String baseUrl = null;
        int retryCount = 0;
        int maxRetries = serviceDiscovery.getAvailableInstances("user").size();

        while (retryCount < maxRetries) {
            try {
                baseUrl = serviceDiscovery.getNextInstance("user");
                String url = baseUrl + "/users" + path +getQueryString(request);
                log.info("ProxyService: forwarding {} request to USerService: {}", method, url);

                ResponseEntity<String> response = forward(url, method, body, headerNames, request);
                log.info("ProxyService: UserService responded with status: {}", response.getStatusCode());
                return response;
            } catch (Exception e) {
                log.warn("ProxyService: UserService instance {} failed: {}", baseUrl, e.getMessage());
                serviceDiscovery.markInstanceAsFailed("user", baseUrl);
                retryCount++;

                if (retryCount >= maxRetries) {
                    throw new RuntimeException("all UserService isntances are failed", e);
                }
            }
        }

        throw new RuntimeException("Failed to forward request to UserService after retries");
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "fallbackNotificationService")
    public ResponseEntity<String> forwardToNotificationService(String path, String method, String body,
                                                                Enumeration<String> headerNames, HttpServletRequest request) {
        String baseUrl = null;
        int retryCount = 0;
        int maxRetries = serviceDiscovery.getAvailableInstances("notification").size();

        while (retryCount < maxRetries) {
            try {
                baseUrl = serviceDiscovery.getNextInstance("notification");
                String url = baseUrl + "/notifications" + path + getQueryString(request);
                log.info("ProxyService: forwarding {} request to NotificationService: {}", method, url);

                ResponseEntity<String> response = forward(url, method, body, headerNames, request);
                log.info("ProxyService: NotifictionService responded with status: {}", response.getStatusCode());
                return response;
            } catch (Exception e) {
                log.warn("ProxyService: NotificationService instance {} failed: {}", baseUrl, e.getMessage());
                serviceDiscovery.markInstanceAsFailed("notification", baseUrl);
                retryCount++;

                if (retryCount >= maxRetries) {
                    throw new RuntimeException("all NotificationService isntances are failed", e);
                }
            }
        }

        throw new RuntimeException("Failed to forward request to NotificationService after retries");
    }

    private ResponseEntity<String> forward(String url, String method, String body,
                                           Enumeration<String> headerNames, HttpServletRequest request) {
        if (restTemplate == null) {
            throw new IllegalStateException("RestTemplate not initialized");
        }
        HttpHeaders headers = new HttpHeaders();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                headers.add(name, request.getHeader(name));
            }
        }

        HttpEntity<String> entity = body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

        return switch (method.toUpperCase()) {
            case "GET" -> restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            case "POST" -> restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            case "PUT" -> restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            case "DELETE" -> restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            default -> ResponseEntity.status(405).body("method is not allowed");
        };
    }

    private String getQueryString(HttpServletRequest request) {
        String qeury = request.getQueryString();
        return qeury != null ? "?" + qeury : "";
    }

    private ResponseEntity<String> fallbackUserService(String path, String method, String body,
                                                       Enumeration<String> headerNames, HttpServletRequest request,
                                                       Throwable t) {
        log.warn("ProxyService: Circuit Breaker fallback for UserService - path: {}, reason: {}", path, t != null ? t.getMessage() : "unknown error");
        return ResponseEntity.status(503).body("UserService is unavailable: " + (t != null ? t.getMessage() : "unknown error"));
    }

    private ResponseEntity<String> fallbackNotificationService(String path, String method, String body,
                                                               Enumeration<String> headerNames, HttpServletRequest request,
                                                               Throwable t) {
        log.warn("ProxyService: Circuit Breaker fallback for NotificationService - path: {}, reason: {}", path, t != null ? t.getMessage() : "unknown error");
        return ResponseEntity.status(503).body("NotificationService is unavailable: " + (t != null ? t.getMessage() : "unknown error"));
    }
}
