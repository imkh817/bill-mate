package com.billmate.slack.install;

import com.slack.api.bolt.model.Bot;
import com.slack.api.bolt.model.Installer;
import com.slack.api.bolt.model.builtin.DefaultBot;
import com.slack.api.bolt.service.InstallationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoltInstallationService implements InstallationService {

    private final SlackInstallationRepository installationRepository;

    @Override
    public boolean isHistoricalDataEnabled() {
        return false;
    }

    @Override
    public void setHistoricalDataEnabled(boolean enabled) {
    }

    @Override
    public void saveInstallerAndBot(Installer installer) {
        // OAuthCallbackController에서 직접 저장하므로 여기서는 불필요
    }

    @Override
    public void deleteBot(Bot bot) {
    }

    @Override
    public void deleteInstaller(Installer installer) {
    }

    @Override
    public Bot findBot(String enterpriseId, String teamId) {
        return installationRepository.findByTeamId(teamId)
                .map(inst -> {
                    DefaultBot bot = new DefaultBot();
                    bot.setTeamId(inst.getTeamId());
                    bot.setBotUserId(inst.getBotUserId());
                    bot.setBotAccessToken(inst.getBotToken());
                    bot.setInstalledAt(System.currentTimeMillis());
                    return (Bot) bot;
                })
                .orElse(null);
    }

    @Override
    public Installer findInstaller(String enterpriseId, String teamId, String userId) {
        return null;
    }
}
