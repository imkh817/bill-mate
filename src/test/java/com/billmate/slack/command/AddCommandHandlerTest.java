package com.billmate.slack.command;

import com.billmate.domain.subscription.dto.SubscriptionResponse;
import com.billmate.domain.subscription.entity.BillingCycle;
import com.billmate.domain.subscription.entity.SubscriptionCategory;
import com.billmate.domain.subscription.service.SubscriptionService;
import com.billmate.domain.user.entity.User;
import com.billmate.domain.user.service.UserService;
import com.slack.api.RequestConfigurator;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AddCommandHandlerTest {

    @Mock
    UserService userService;

    @Mock
    SubscriptionService subscriptionService;

    @InjectMocks
    AddCommandHandler addCommandHandler;

    @Mock
    SlashCommandRequest req;

    @Mock
    SlashCommandPayload payload;

    @Mock
    SlashCommandContext ctx;

    @Mock
    MethodsClient client;

    @BeforeEach
    void setUp() throws Exception {
        given(req.getPayload()).willReturn(payload);
        given(payload.getUserId()).willReturn("U001");
        given(payload.getTeamId()).willReturn("T001");
        given(payload.getUserName()).willReturn("alice");
        given(ctx.client()).willReturn(client);
        given(client.chatPostMessage(ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any()))
                .willReturn(new ChatPostMessageResponse());
    }

    @Test
    @DisplayName("UC-AH1: 올바른 텍스트 입력 → subscriptionService.create() 호출됨")
    void sendAsync_validInput_createCalled() throws Exception {
        given(payload.getText()).willReturn("add \"Netflix\" OTT 15900 25");
        User user = User.builder().slackUserId("U001").slackWorkspaceId("T001").displayName("alice").build();
        given(userService.getOrCreateUser("U001", "T001", "alice")).willReturn(user);
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(1L).serviceName("Netflix").category(SubscriptionCategory.OTT)
                .amount(new BigDecimal("15900")).currency("KRW").billingDay(25)
                .billingCycle(BillingCycle.MONTHLY).startedAt(LocalDate.now()).build();
        given(subscriptionService.create(eq(user), any())).willReturn(response);

        addCommandHandler.sendAsync(req, ctx);

        then(subscriptionService).should(times(1)).create(eq(user), any());
    }

    @Test
    @DisplayName("UC-AH2: 인수 부족 텍스트 → 에러 메시지 chatPostMessage 호출")
    void sendAsync_insufficientArgs_errorMessage() throws Exception {
        given(payload.getText()).willReturn("add Netflix");

        addCommandHandler.sendAsync(req, ctx);

        then(client).should(times(1)).chatPostMessage(ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any());
        then(subscriptionService).should(never()).create(any(), any());
    }

    @Test
    @DisplayName("UC-AH3: 잘못된 카테고리 → IllegalArgumentException catch 후 에러 메시지")
    void sendAsync_invalidCategory_errorMessage() throws Exception {
        given(payload.getText()).willReturn("add \"Netflix\" INVALID_CAT 15900 25");
        // SubscriptionCategory.valueOf("INVALID_CAT") will throw IllegalArgumentException

        addCommandHandler.sendAsync(req, ctx);

        then(client).should(atLeastOnce()).chatPostMessage(ArgumentMatchers.<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>any());
        then(subscriptionService).should(never()).create(any(), any());
    }
}
