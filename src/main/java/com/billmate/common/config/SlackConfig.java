package com.billmate.common.config;

import com.billmate.slack.install.BoltInstallationService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {

    @Value("${slack.signing-secret}")
    private String signingSecret;

    @Bean
    public App slackApp(BoltInstallationService installationService) {
        AppConfig appConfig = AppConfig.builder()
                .signingSecret(signingSecret)
                .build();
        return new App(appConfig).service(installationService);
    }

    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackServlet(App app) throws Exception {
        SlackAppServlet servlet = new SlackAppServlet(app);
        return new ServletRegistrationBean<>(servlet, "/slack/command", "/slack/events");
    }
}
