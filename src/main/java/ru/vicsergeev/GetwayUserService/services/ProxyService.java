package ru.vicsergeev.GetwayUserService.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;

/**
 * Created by Victor 31.10.2025
 */

@Service
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "services")
public class ProxyService {
    @Setter
    private String userBaseURL;
    @Setter
    private String notificationServiceBaseURL;

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> forwardToUserService(String path, String method, String body,
                                                       Enumeration<String> headerNames, HttpServletRequest request) {
       String url = userBaseURL + "/users" + path + getQueryString(request);
       return forward(url, method, body, headerNames, request);
    }

    public ResponseEntity<String> forwardToNotificationService(String path, String method, String body,
                                                                Enumeration<String> headerNames, HttpServletRequest request) {
        String url = notificationServiceBaseURL + "/notifications";
        return forward(url, method, body, headerNames, request);
    }

    private ResponseEntity<String> forward(String url, String method, String body,
                                           Enumeration<String> headerNames, HttpServletRequest request) {
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
}
