package space.gilberthg.springwebsocketimpl.chat;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChatService {

    private final RoomCatalog roomCatalog;
    private final InMemoryChatStore chatStore;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            RoomCatalog roomCatalog,
            InMemoryChatStore chatStore,
            SimpMessagingTemplate messagingTemplate) {
        this.roomCatalog = roomCatalog;
        this.chatStore = chatStore;
        this.messagingTemplate = messagingTemplate;
    }

    public ChatMessage send(String roomId, String sender, String content) {
        roomCatalog.require(roomId);
        ChatMessage message = new ChatMessage(UUID.randomUUID().toString(), roomId, sender, content, Instant.now());
        chatStore.append(message);
        messagingTemplate.convertAndSend("/topic/rooms." + roomId, message);
        return message;
    }

    public void typing(String roomId, String username, boolean typing) {
        roomCatalog.require(roomId);
        messagingTemplate.convertAndSend(
                "/topic/rooms." + roomId + ".typing",
                new TypingEvent(username, typing));
    }
}
