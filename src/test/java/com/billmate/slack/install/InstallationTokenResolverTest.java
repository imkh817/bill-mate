package com.billmate.slack.install;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InstallationTokenResolverTest {

    @Mock
    SlackInstallationRepository installationRepository;

    @InjectMocks
    InstallationTokenResolver tokenResolver;

    @Test
    @DisplayName("UC-TR1: teamId로 설치 정보 조회 시 botToken 반환")
    void getBotToken_found_returnsToken() {
        SlackInstallation installation = SlackInstallation.builder()
                .teamId("T001")
                .teamName("Test Team")
                .botToken("xoxb-test-token")
                .botUserId("B001")
                .installedByUserId("U001")
                .build();
        given(installationRepository.findByTeamId("T001")).willReturn(Optional.of(installation));

        String token = tokenResolver.getBotToken("T001");

        assertThat(token).isEqualTo("xoxb-test-token");
    }

    @Test
    @DisplayName("UC-TR2: 등록되지 않은 teamId → IllegalStateException")
    void getBotToken_notFound_throwsIllegalStateException() {
        given(installationRepository.findByTeamId("T_UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenResolver.getBotToken("T_UNKNOWN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("T_UNKNOWN");
    }

    @Test
    @DisplayName("UC-TR3: 서로 다른 두 teamId → 각각 다른 토큰 반환")
    void getBotToken_multipleTeams_returnsCorrectToken() {
        SlackInstallation inst1 = SlackInstallation.builder()
                .teamId("T001").botToken("xoxb-token-1").teamName("Team A")
                .botUserId("B001").installedByUserId("U001").build();
        SlackInstallation inst2 = SlackInstallation.builder()
                .teamId("T002").botToken("xoxb-token-2").teamName("Team B")
                .botUserId("B002").installedByUserId("U002").build();

        given(installationRepository.findByTeamId("T001")).willReturn(Optional.of(inst1));
        given(installationRepository.findByTeamId("T002")).willReturn(Optional.of(inst2));

        assertThat(tokenResolver.getBotToken("T001")).isEqualTo("xoxb-token-1");
        assertThat(tokenResolver.getBotToken("T002")).isEqualTo("xoxb-token-2");
    }
}
