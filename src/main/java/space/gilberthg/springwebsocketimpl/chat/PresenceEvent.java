package space.gilberthg.springwebsocketimpl.chat;

import java.util.List;

public record PresenceEvent(String roomId, List<String> users) {
}
