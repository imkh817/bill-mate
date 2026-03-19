package com.billmate.slack;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationStateStore {

    private final ConcurrentHashMap<String, ConversationState> store = new ConcurrentHashMap<>();

    public ConversationState get(String userId) {
        return store.get(userId);
    }

    public void put(String userId, ConversationState state) {
        store.put(userId, state);
    }

    public void remove(String userId) {
        store.remove(userId);
    }
}
