package space.gilberthg.springwebsocketimpl.chat;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryChatStore {

    private static final int MAX_MESSAGES = 100;

    private final Map<String, Deque<ChatMessage>> messages = new ConcurrentHashMap<>();

    public ChatMessage append(ChatMessage message) {
        Deque<ChatMessage> history = messages.computeIfAbsent(message.roomId(), key -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(message);
            while (history.size() > MAX_MESSAGES) {
                history.removeFirst();
            }
        }
        return message;
    }

    public List<ChatMessage> history(String roomId) {
        Deque<ChatMessage> history = messages.get(roomId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }
}
