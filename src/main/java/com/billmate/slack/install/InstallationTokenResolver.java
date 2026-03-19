package com.billmate.slack.install;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstallationTokenResolver {

    private final SlackInstallationRepository installationRepository;

    public String getBotToken(String teamId) {
        return installationRepository.findByTeamId(teamId)
                .map(SlackInstallation::getBotToken)
                .orElseThrow(() -> new IllegalStateException("No installation found for team: " + teamId));
    }
}
