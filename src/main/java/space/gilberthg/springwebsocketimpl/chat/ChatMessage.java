package space.gilberthg.springwebsocketimpl.chat;

public record ChatMessage(String id, String roomId, String sender, String content, java.time.Instant sentAt) {
}
