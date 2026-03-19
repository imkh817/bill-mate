package com.billmate.slack;

import com.billmate.domain.user.service.UserService;
import com.billmate.slack.install.SlackInstallation;
import com.billmate.slack.install.SlackInstallationRepository;
import com.billmate.slack.message.SlackMessageBuilder;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Slf4j
@RestController
public class OAuthCallbackController {

    private final SlackInstallationRepository installationRepository;
    private final UserService userService;
    private final App slackApp;
    private final RestClient slackRestClient;

    @Value("${slack.client-id}")
    private String clientId;

    @Value("${slack.client-secret}")
    private String clientSecret;

    public OAuthCallbackController(SlackInstallationRepository installationRepository,
                                   UserService userService,
                                   App slackApp,
                                   @Qualifier("slackApiRestClient") RestClient slackRestClient) {
        this.installationRepository = installationRepository;
        this.userService = userService;
        this.slackApp = slackApp;
        this.slackRestClient = slackRestClient;
    }

    @GetMapping({"/slack/install", "/slack/oauth/install"})
    public RedirectView install() {
        String scopes = "chat:write,commands,im:history,im:read,im:write";
        String url = "https://slack.com/oauth/v2/authorize?client_id=" + clientId + "&scope=" + scopes;
        return new RedirectView(url);
    }

    @GetMapping("/slack/oauth/callback")
    public RedirectView handleCallback(@RequestParam String code) {
        try {
            Map<?, ?> response = slackRestClient.post()
                    .uri("/api/oauth.v2.access")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("client_id=" + clientId
                            + "&client_secret=" + clientSecret
                            + "&code=" + code)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                log.error("OAuth access failed: {}", response);
                return new RedirectView("/slack/install/error");
            }

            String teamId = extractTeamId(response);
            String teamName = extractTeamName(response);
            String botToken = extractBotToken(response);
            String botUserId = extractBotUserId(response);
            String installedByUserId = extractInstalledByUserId(response);

            installationRepository.findByTeamId(teamId)
                    .map(existing -> updateInstallation(existing, teamName, botToken, botUserId, installedByUserId))
                    .orElseGet(() -> installationRepository.save(
                            SlackInstallation.builder()
                                    .teamId(teamId)
                                    .teamName(teamName)
                                    .botToken(botToken)
                                    .botUserId(botUserId)
                                    .installedByUserId(installedByUserId)
                                    .build()));

            userService.getOrCreateUser(installedByUserId, teamId, null);

            sendWelcomeMessage(botToken, installedByUserId);

            log.info("Slack app installed for team={} by user={}", teamId, installedByUserId);
        } catch (Exception e) {
            log.error("OAuth callback error", e);
            return new RedirectView("/slack/install/error");
        }

        return new RedirectView("/install-success.html");
    }

    @GetMapping("/slack/install/success")
    public RedirectView success() {
        return new RedirectView("/install-success.html");
    }

    @GetMapping("/slack/install/error")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String error() {
        return "설치 중 오류가 발생했습니다. 다시 시도해주세요.";
    }

    private void sendWelcomeMessage(String botToken, String userId) {
        try {
            slackApp.client().chatPostMessage(r -> r
                    .token(botToken)
                    .channel(userId)
                    .text("bill-mate에 오신 것을 환영합니다!")
                    .blocks(SlackMessageBuilder.buildOnboarding()));
        } catch (Exception e) {
            log.error("Failed to send welcome DM to user={}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTeamId(Map<?, ?> response) {
        Map<?, ?> team = (Map<?, ?>) response.get("team");
        return team != null ? (String) team.get("id") : null;
    }

    @SuppressWarnings("unchecked")
    private String extractTeamName(Map<?, ?> response) {
        Map<?, ?> team = (Map<?, ?>) response.get("team");
        return team != null ? (String) team.get("name") : null;
    }

    private String extractBotToken(Map<?, ?> response) {
        return (String) response.get("access_token");
    }

    private String extractBotUserId(Map<?, ?> response) {
        return (String) response.get("bot_user_id");
    }

    @SuppressWarnings("unchecked")
    private String extractInstalledByUserId(Map<?, ?> response) {
        Map<?, ?> authedUser = (Map<?, ?>) response.get("authed_user");
        return authedUser != null ? (String) authedUser.get("id") : null;
    }

    private SlackInstallation updateInstallation(SlackInstallation existing,
                                                  String teamName, String botToken,
                                                  String botUserId, String installedByUserId) {
        installationRepository.delete(existing);
        return installationRepository.save(
                SlackInstallation.builder()
                        .teamId(existing.getTeamId())
                        .teamName(teamName)
                        .botToken(botToken)
                        .botUserId(botUserId)
                        .installedByUserId(installedByUserId)
                        .build());
    }
}
