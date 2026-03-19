package com.billmate.domain.user.service;

import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("UC-US1: workspaceId 있을 때 신규 유저 → findBySlackUserIdAndSlackWorkspaceId 조회 후 save")
    void getOrCreateUser_withWorkspaceId_newUser_savesCalled() {
        given(userRepository.findBySlackUserIdAndSlackWorkspaceId("U001", "W001")).willReturn(Optional.empty());
        User savedUser = User.builder().slackUserId("U001").slackWorkspaceId("W001").displayName("Alice").build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        User result = userService.getOrCreateUser("U001", "W001", "Alice");

        then(userRepository).should().findBySlackUserIdAndSlackWorkspaceId("U001", "W001");
        then(userRepository).should(never()).findBySlackUserId(anyString());
        then(userRepository).should().save(any(User.class));
        assertThat(result.getSlackUserId()).isEqualTo("U001");
    }

    @Test
    @DisplayName("UC-US2: workspaceId 있을 때 기존 유저 → save 호출 없이 기존 유저 반환")
    void getOrCreateUser_withWorkspaceId_existingUser_noSave() {
        User existingUser = User.builder().slackUserId("U001").slackWorkspaceId("W001").displayName("Alice").build();
        given(userRepository.findBySlackUserIdAndSlackWorkspaceId("U001", "W001")).willReturn(Optional.of(existingUser));

        User result = userService.getOrCreateUser("U001", "W001", "Alice");

        then(userRepository).should(never()).save(any());
        assertThat(result).isSameAs(existingUser);
    }

    @Test
    @DisplayName("UC-US3: workspaceId null → findBySlackUserId fallback 사용")
    void getOrCreateUser_withoutWorkspaceId_fallbackToSlackUserId() {
        given(userRepository.findBySlackUserId("U001")).willReturn(Optional.empty());
        User savedUser = User.builder().slackUserId("U001").slackWorkspaceId(null).displayName(null).build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        User result = userService.getOrCreateUser("U001", null, null);

        then(userRepository).should().findBySlackUserId("U001");
        then(userRepository).should(never()).findBySlackUserIdAndSlackWorkspaceId(anyString(), anyString());
    }

    @Test
    @DisplayName("UC-US4: 존재하지 않는 slackUserId로 getUser → IllegalArgumentException")
    void getUser_notFound_throwsException() {
        given(userRepository.findBySlackUserId("U_NONE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("U_NONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("U_NONE");
    }

    @Test
    @DisplayName("UC-US5: getOrCreateUser — 저장 시 workspaceId 필드 정확히 설정됨")
    void getOrCreateUser_newUser_workspaceIdSetCorrectly() {
        given(userRepository.findBySlackUserIdAndSlackWorkspaceId("U002", "T_TEAM")).willReturn(Optional.empty());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        given(userRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

        userService.getOrCreateUser("U002", "T_TEAM", "Bob");

        User saved = captor.getValue();
        assertThat(saved.getSlackUserId()).isEqualTo("U002");
        assertThat(saved.getSlackWorkspaceId()).isEqualTo("T_TEAM");
        assertThat(saved.getDisplayName()).isEqualTo("Bob");
    }
}
