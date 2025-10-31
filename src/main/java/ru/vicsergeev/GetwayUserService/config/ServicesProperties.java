package ru.vicsergeev.GetwayUserService.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by Victor 31.10.2025
 */

@Getter
@ConfigurationProperties(prefix = "services")
public class ServicesProperties {
    private final ServiceProps user = new ServiceProps();
    private final ServiceProps notification = new ServiceProps();

    @Getter
    public static class ServiceProps {
        private String baseUrl;

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
