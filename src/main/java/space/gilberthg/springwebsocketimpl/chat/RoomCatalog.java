package space.gilberthg.springwebsocketimpl.chat;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RoomCatalog {

    private final Map<String, Room> rooms = List.of(
                    new Room("general", "General"),
                    new Room("engineering", "Engineering"),
                    new Room("support", "Support"))
            .stream()
            .collect(Collectors.toUnmodifiableMap(Room::id, Function.identity()));

    public List<Room> list() {
        return List.of(rooms.get("general"), rooms.get("engineering"), rooms.get("support"));
    }

    public Optional<Room> find(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public void require(String roomId) {
        if (!rooms.containsKey(roomId)) {
            throw new UnknownRoomException(roomId);
        }
    }
}
