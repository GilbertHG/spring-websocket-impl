package space.gilberthg.springwebsocketimpl.chat;

public record TypingEvent(String username, boolean typing) {
}
