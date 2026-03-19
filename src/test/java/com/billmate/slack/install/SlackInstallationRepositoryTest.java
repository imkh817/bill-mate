package com.billmate.slack.install;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SlackInstallationRepositoryTest {

    @Autowired
    SlackInstallationRepository installationRepository;

    @Autowired
    TestEntityManager em;

    private SlackInstallation installation(String teamId) {
        return SlackInstallation.builder()
                .teamId(teamId)
                .teamName("Team " + teamId)
                .botToken("xoxb-" + teamId)
                .botUserId("B001")
                .installedByUserId("U001")
                .build();
    }

    @Test
    @DisplayName("UC-IR1: 저장 후 findByTeamId → 올바른 설치 정보 반환")
    void saveAndFindByTeamId() {
        installationRepository.save(installation("T001"));
        em.flush();
        em.clear();

        Optional<SlackInstallation> found = installationRepository.findByTeamId("T001");

        assertThat(found).isPresent();
        assertThat(found.get().getTeamId()).isEqualTo("T001");
        assertThat(found.get().getBotToken()).isEqualTo("xoxb-T001");
        assertThat(found.get().getTeamName()).isEqualTo("Team T001");
    }

    @Test
    @DisplayName("UC-IR2: 존재하지 않는 teamId → Optional.empty 반환")
    void findByTeamId_notFound_returnsEmpty() {
        Optional<SlackInstallation> found = installationRepository.findByTeamId("T_NONE");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("UC-IR3: 동일 teamId로 중복 저장 시 unique 제약 위반")
    void duplicateTeamId_throwsException() {
        installationRepository.save(installation("T_DUP"));
        em.flush();

        assertThatThrownBy(() -> installationRepository.saveAndFlush(installation("T_DUP")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UC-IR4: 서로 다른 teamId → 각각 독립적으로 저장/조회됨 (멀티 테넌트)")
    void multipleTeams_storedAndRetrievedIndependently() {
        installationRepository.save(installation("T_A"));
        installationRepository.save(installation("T_B"));
        em.flush();
        em.clear();

        assertThat(installationRepository.findByTeamId("T_A").get().getBotToken()).isEqualTo("xoxb-T_A");
        assertThat(installationRepository.findByTeamId("T_B").get().getBotToken()).isEqualTo("xoxb-T_B");
    }
}
