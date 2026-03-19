package com.billmate.domain.subscription.entity;

public enum SubscriptionCategory {
    OTT,
    MUSIC_STREAMING,
    CLOUD_STORAGE,
    PRODUCTIVITY,
    DEVELOPER_TOOL,
    AI_TOOL,
    GAME,
    FITNESS,
    NEWS,
    EDUCATION,
    OTHER;

    public String displayName() {
        return switch (this) {
            case OTT -> "동영상";
            case MUSIC_STREAMING -> "음악";
            case CLOUD_STORAGE -> "클라우드";
            case PRODUCTIVITY -> "생산성";
            case DEVELOPER_TOOL -> "개발 도구";
            case AI_TOOL -> "AI";
            case GAME -> "게임";
            case FITNESS -> "피트니스";
            case NEWS -> "뉴스";
            case EDUCATION -> "교육";
            case OTHER -> "기타";
        };
    }
}
