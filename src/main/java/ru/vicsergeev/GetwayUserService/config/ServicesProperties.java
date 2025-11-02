package ru.vicsergeev.GetwayUserService.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor 31.10.2025
 */

@Getter
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {
    private final ServiceProps user = new ServiceProps();
    private final ServiceProps notification = new ServiceProps();

    @Getter
    @Setter
    public static class ServiceProps {
        private String baseUrl;
        private List<String> instances = new ArrayList<>();
        private int connectTimeout = 2000;
        private int responseTimeout = 5000;
        private int healthCheckInterval = 30;
        private int healthCheckTimeout = 3000;

        public List<String> getInstancesOrSingle() {
            if (instances != null && !instances.isEmpty()) {
                return instances;
            }
            List<String> single = new ArrayList<>();
            if (baseUrl != null && !baseUrl.isEmpty()) {
                single.add(baseUrl);
            }
            return single;
        }
    }
}
