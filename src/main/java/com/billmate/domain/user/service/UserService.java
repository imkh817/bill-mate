package com.billmate.domain.user.service;

import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String slackUserId, String slackWorkspaceId, String displayName) {
        if (slackWorkspaceId != null) {
            return userRepository.findBySlackUserIdAndSlackWorkspaceId(slackUserId, slackWorkspaceId)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .slackUserId(slackUserId)
                                    .slackWorkspaceId(slackWorkspaceId)
                                    .displayName(displayName)
                                    .build()
                    ));
        }
        return userRepository.findBySlackUserId(slackUserId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .slackUserId(slackUserId)
                                .slackWorkspaceId(slackWorkspaceId)
                                .displayName(displayName)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public User getUser(String slackUserId) {
        return userRepository.findBySlackUserId(slackUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + slackUserId));
    }
}
