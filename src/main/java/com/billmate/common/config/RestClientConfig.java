package com.billmate.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${quickchart.base-url}")
    private String quickchartBaseUrl;

    @Bean
    public RestClient quickchartRestClient() {
        return RestClient.builder()
                .baseUrl(quickchartBaseUrl)
                .build();
    }

    @Bean
    public RestClient slackApiRestClient() {
        return RestClient.builder()
                .baseUrl("https://slack.com")
                .build();
    }
}
