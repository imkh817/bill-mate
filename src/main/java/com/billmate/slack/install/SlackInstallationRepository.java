package com.billmate.slack.install;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlackInstallationRepository extends JpaRepository<SlackInstallation, Long> {
    Optional<SlackInstallation> findByTeamId(String teamId);
}
