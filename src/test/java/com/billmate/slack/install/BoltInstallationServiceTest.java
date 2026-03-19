package com.billmate.slack.install;

import com.slack.api.bolt.model.Bot;
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
class BoltInstallationServiceTest {

    @Mock
    SlackInstallationRepository installationRepository;

    @InjectMocks
    BoltInstallationService boltInstallationService;

    @Test
    @DisplayName("UC-BIS1: findBot — 등록된 teamId → botAccessToken 포함한 Bot 반환")
    void findBot_found_returnsBotWithToken() {
        SlackInstallation installation = SlackInstallation.builder()
                .teamId("T001")
                .teamName("Test Team")
                .botToken("xoxb-test-token")
                .botUserId("B001")
                .installedByUserId("U001")
                .build();
        given(installationRepository.findByTeamId("T001")).willReturn(Optional.of(installation));

        Bot bot = boltInstallationService.findBot(null, "T001");

        assertThat(bot).isNotNull();
        assertThat(bot.getBotAccessToken()).isEqualTo("xoxb-test-token");
        assertThat(bot.getBotUserId()).isEqualTo("B001");
        assertThat(bot.getTeamId()).isEqualTo("T001");
    }

    @Test
    @DisplayName("UC-BIS2: findBot — 미등록 teamId → null 반환 (Bolt가 unauthorized 처리)")
    void findBot_notFound_returnsNull() {
        given(installationRepository.findByTeamId("T_UNKNOWN")).willReturn(Optional.empty());

        Bot bot = boltInstallationService.findBot(null, "T_UNKNOWN");

        assertThat(bot).isNull();
    }

    @Test
    @DisplayName("UC-BIS3: findInstaller → null 반환 (봇 전용, installer 불필요)")
    void findInstaller_returnsNull() {
        assertThat(boltInstallationService.findInstaller(null, "T001", "U001")).isNull();
    }

    @Test
    @DisplayName("UC-BIS4: saveInstallerAndBot → 예외 없이 no-op 처리")
    void saveInstallerAndBot_noException() {
        assertThatCode(() -> boltInstallationService.saveInstallerAndBot(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UC-BIS5: isHistoricalDataEnabled → false")
    void isHistoricalDataEnabled_returnsFalse() {
        assertThat(boltInstallationService.isHistoricalDataEnabled()).isFalse();
    }
}
