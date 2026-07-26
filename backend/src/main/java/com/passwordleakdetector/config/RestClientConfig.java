package com.passwordleakdetector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient hibpRestClient(RestClient.Builder builder,
                                      @Value("${app.hibp.base-url}") String baseUrl,
                                      @Value("${app.hibp.connect-timeout-ms}") long connectTimeoutMs,
                                      @Value("${app.hibp.read-timeout-ms}") long readTimeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
