package ru.vicsergeev.GetwayUserService.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.vicsergeev.GetwayUserService.config.ServicesProperties;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Victor 01.11.2025
 */

@Component
public class ServiceDiscoveryManager {
    private static final Logger log = LoggerFactory.getLogger(ServiceDiscoveryManager.class);
    private final ServicesProperties serviceProperties;
    private final RestTemplate healthCheckRestTemplate;
    private long healthCheckIntervalMs;

    private final Map<String, Map<String, Boolean>> serviceInstances = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> roundRobinIndexes = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Long>> instancesFailureTimes = new ConcurrentHashMap<>();

    private static final long FAILURE_COOLDOWN_IN_MS = 30000;

    public ServiceDiscoveryManager(ServicesProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
        this.healthCheckRestTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        // init interval health check from config
        healthCheckIntervalMs = serviceProperties.getUser().getHealthCheckInterval() * 1000L;
        // init user services inst
        List<String> userInstances = serviceProperties.getUser().getInstancesOrSingle();
        serviceInstances.put("user", new ConcurrentHashMap<>());
        instancesFailureTimes.put("user", new ConcurrentHashMap<>());
        roundRobinIndexes.put("user", new AtomicInteger(0));

        for (String instance : userInstances) {
            serviceInstances.get("user").put(instance, true);
            log.info("service discovery info msg: registered user service instance: {}", instance);
        }

        // init notification service inst
        List<String> notificationIntances = serviceProperties.getNotification().getInstancesOrSingle();
        serviceInstances.put("notification", new ConcurrentHashMap<>());
        instancesFailureTimes.put("notification", new ConcurrentHashMap<>());
        roundRobinIndexes.put("notification", new AtomicInteger(0));

        for (String instance : notificationIntances) {
            serviceInstances.get("notification").put(instance, true);
            log.info("service discovery info msg: registered user service instance: {}", instance);
        }
    }

    // round robin to get next available instance
    public String getNextInstance(String serviceName) {
        Map<String, Boolean> instances = serviceInstances.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("no instances available for service: " + serviceName);
        }

        List<String> availableInstances = instances.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();

        if (availableInstances.isEmpty()) {
            // if all instances are unavailable try to recover
            log.warn("ServiceDiscovery: all instances unavalable for {}, trying to recover", serviceName);
            availableInstances = new ArrayList<>(instances.keySet());
        }

        AtomicInteger index = roundRobinIndexes.get(serviceName);
        int currentIndex = index.getAndIncrement() % availableInstances.size();
        String selectedInstance = availableInstances.get(currentIndex);

        log.debug("ServiceDiscovery: selected {} instance: {} (index: {})", serviceName, selectedInstance, currentIndex);

        return selectedInstance;
    }

    // mark inst as failed
    public void markInstanceAsFailed(String serviceName, String instanceUrl) {
        Map<String, Boolean> instances = serviceInstances.get(serviceName);
        if (instances != null && instances.containsKey(instanceUrl)) {
            instances.put(instanceUrl, false);
            instancesFailureTimes.get(serviceName).put(instanceUrl, System.currentTimeMillis());
            log.warn("ServiceDiscovery: marked {} instance as unvailable: {}", serviceName, instanceUrl);
        }
    }

    // periodic instances health check
    // fixedDelay - wil restart after specific interval after previous terminated
    @Scheduled(fixedDelay = 30000)
    public void performHealthChecks() {
        checkServiceInstances("user", "/users");
        checkServiceInstances("notification", "/actuator/health");
    }

    private void checkServiceInstances(String serviceName, String healthEndpoint) {
        Map<String, Boolean> instances = serviceInstances.get(serviceName);
        if (instances == null) {
            return;
        }

        ServicesProperties.ServiceProps serviceProps = "user".equals(serviceName) ?
                serviceProperties.getUser() : serviceProperties.getNotification();

        int timeout = serviceProps.getHealthCheckTimeout();

        for (Map.Entry<String, Boolean> entry : instances.entrySet()) {
            String instanceUrl = entry.getKey();
            boolean isCurrentAvailable = entry.getValue();

            try {
                String healthUrl = instanceUrl + healthEndpoint;
                ResponseEntity<String> response = healthCheckRestTemplate.exchange(
                        healthUrl,
                        HttpMethod.GET,
                        null,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    if (!isCurrentAvailable) {
                        log.info("ServiceDiscovery: {} instance recovered: {}", serviceName, instanceUrl);
                        instances.put(instanceUrl, true);
                        instancesFailureTimes.get(serviceName).remove(instanceUrl);
                    }
                } else {
                    if (isCurrentAvailable) {
                        markInstanceAsFailed(serviceName, instanceUrl);
                    }
                }
            } catch (Exception e) {
                // check if cooldown period passed
                Map<String, Long> failures = instancesFailureTimes.get(serviceName);
                Long failureTime = failures.get(instanceUrl);

                if (failureTime == null || (System.currentTimeMillis() - failureTime) > FAILURE_COOLDOWN_IN_MS) {
                    if (isCurrentAvailable) {
                        markInstanceAsFailed(serviceName, instanceUrl);
                    }
                    log.debug("ServiceDiscovery: {} isnatnce {} health chak failed: {}", serviceName, instanceUrl, e.getMessage());
                }
            }
        }
    }

    // get all available isntances
    public List<String> getAvailableInstances(String serviceName) {
        Map<String, Boolean> instances = serviceInstances.get(serviceName);
        if (instances == null) {
            return Collections.emptyList();
        }

        return instances.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
    }
}
