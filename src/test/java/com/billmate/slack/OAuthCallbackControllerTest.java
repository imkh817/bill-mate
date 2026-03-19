package com.billmate.slack;

import com.billmate.domain.user.service.UserService;
import com.billmate.slack.install.SlackInstallation;
import com.billmate.slack.install.SlackInstallationRepository;
import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.view.RedirectView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuthCallbackControllerTest {

    @Mock
    SlackInstallationRepository installationRepository;

    @Mock
    UserService userService;

    @Mock
    App slackApp;

    @Mock
    RestClient slackRestClient;

    @Mock
    RestClient.RequestBodySpec requestBodySpec;

    @Mock
    RestClient.ResponseSpec responseSpec;

    @Mock
    RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    MethodsClient methodsClient;

    @InjectMocks
    OAuthCallbackController controller;

    @BeforeEach
    void setUp() throws Exception {
        given(slackRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.body(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);

        given(slackApp.client()).willReturn(methodsClient);
        given(methodsClient.chatPostMessage(
                ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()))
                .willReturn(new ChatPostMessageResponse());
    }

    private Map<String, Object> successResponse() {
        Map<String, Object> team = new LinkedHashMap<>();
        team.put("id", "T001");
        team.put("name", "Test Team");

        Map<String, Object> authedUser = new LinkedHashMap<>();
        authedUser.put("id", "U001");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("access_token", "xoxb-test-token");
        response.put("bot_user_id", "B001");
        response.put("team", team);
        response.put("authed_user", authedUser);
        return response;
    }

    @Test
    @DisplayName("UC-OA1: 정상 OAuth 콜백 → SlackInstallation 저장 후 /slack/install/success 리다이렉트")
    void handleCallback_success_savesInstallationAndRedirects() {
        given(responseSpec.body(Map.class)).willReturn(successResponse());
        given(installationRepository.findByTeamId("T001")).willReturn(Optional.empty());
        given(installationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RedirectView result = controller.handleCallback("valid-code");

        ArgumentCaptor<SlackInstallation> captor = ArgumentCaptor.forClass(SlackInstallation.class);
        then(installationRepository).should().save(captor.capture());

        SlackInstallation saved = captor.getValue();
        assertThat(saved.getTeamId()).isEqualTo("T001");
        assertThat(saved.getTeamName()).isEqualTo("Test Team");
        assertThat(saved.getBotToken()).isEqualTo("xoxb-test-token");
        assertThat(saved.getBotUserId()).isEqualTo("B001");
        assertThat(saved.getInstalledByUserId()).isEqualTo("U001");

        assertThat(result.getUrl()).isEqualTo("/slack/install/success");
    }

    @Test
    @DisplayName("UC-OA2: 정상 설치 → 설치한 유저 getOrCreateUser 호출")
    void handleCallback_success_createsUser() {
        given(responseSpec.body(Map.class)).willReturn(successResponse());
        given(installationRepository.findByTeamId("T001")).willReturn(Optional.empty());
        given(installationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        controller.handleCallback("valid-code");

        then(userService).should().getOrCreateUser("U001", "T001", null);
    }

    @Test
    @DisplayName("UC-OA3: 정상 설치 → 환영 DM 전송")
    void handleCallback_success_sendsWelcomeDm() throws Exception {
        given(responseSpec.body(Map.class)).willReturn(successResponse());
        given(installationRepository.findByTeamId("T001")).willReturn(Optional.empty());
        given(installationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        controller.handleCallback("valid-code");

        then(methodsClient).should().chatPostMessage(
                ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any());
    }

    @Test
    @DisplayName("UC-OA4: Slack API ok=false → /slack/install/error 리다이렉트")
    void handleCallback_slackApiError_redirectsToError() {
        Map<String, Object> errorResponse = Map.of("ok", false, "error", "invalid_code");
        given(responseSpec.body(Map.class)).willReturn(errorResponse);

        RedirectView result = controller.handleCallback("bad-code");

        assertThat(result.getUrl()).isEqualTo("/slack/install/error");
        then(installationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("UC-OA5: Slack API 예외 발생 → /slack/install/error 리다이렉트")
    void handleCallback_exception_redirectsToError() {
        given(responseSpec.body(Map.class)).willThrow(new RuntimeException("network error"));

        RedirectView result = controller.handleCallback("any-code");

        assertThat(result.getUrl()).isEqualTo("/slack/install/error");
        then(installationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("UC-OA6: 이미 설치된 워크스페이스 재설치 → 기존 삭제 후 새로 저장")
    void handleCallback_reinstall_deletesOldAndSavesNew() {
        SlackInstallation existing = SlackInstallation.builder()
                .teamId("T001").teamName("Old Team").botToken("xoxb-old")
                .botUserId("B_OLD").installedByUserId("U_OLD").build();

        given(responseSpec.body(Map.class)).willReturn(successResponse());
        given(installationRepository.findByTeamId("T001")).willReturn(Optional.of(existing));
        given(installationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        controller.handleCallback("valid-code");

        then(installationRepository).should().delete(existing);
        ArgumentCaptor<SlackInstallation> captor = ArgumentCaptor.forClass(SlackInstallation.class);
        then(installationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getBotToken()).isEqualTo("xoxb-test-token");
    }
}
