package com.billmate.slack;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConversationState {
    private ConversationStep step;
    private String categoryCode;
    private String customCategoryName;
    private String serviceName;
    private BigDecimal amount;
    private String teamId;
}
