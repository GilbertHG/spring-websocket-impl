package space.gilberthg.springwebsocketimpl.chat;

import space.gilberthg.springwebsocketimpl.presence.PresenceService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomCatalog roomCatalog;
    private final InMemoryChatStore chatStore;
    private final PresenceService presenceService;

    public RoomController(RoomCatalog roomCatalog, InMemoryChatStore chatStore, PresenceService presenceService) {
        this.roomCatalog = roomCatalog;
        this.chatStore = chatStore;
        this.presenceService = presenceService;
    }

    @GetMapping
    public List<Room> list() {
        return roomCatalog.list();
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessage> messages(@PathVariable String roomId) {
        roomCatalog.require(roomId);
        return chatStore.history(roomId);
    }

    @GetMapping("/{roomId}/presence")
    public PresenceEvent presence(@PathVariable String roomId) {
        roomCatalog.require(roomId);
        return new PresenceEvent(roomId, presenceService.usersIn(roomId));
    }
}
