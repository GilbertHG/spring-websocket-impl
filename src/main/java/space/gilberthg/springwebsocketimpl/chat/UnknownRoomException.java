package space.gilberthg.springwebsocketimpl.chat;

public class UnknownRoomException extends RuntimeException {

    public UnknownRoomException(String roomId) {
        super("Unknown room: " + roomId);
    }
}
