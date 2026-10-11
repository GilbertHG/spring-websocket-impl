package space.gilberthg.springwebsocketimpl.presence;

import space.gilberthg.springwebsocketimpl.chat.PresenceEvent;
import space.gilberthg.springwebsocketimpl.chat.RoomCatalog;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks who is subscribed to a room chat topic. JVM-local only — use Redis (or similar)
 * if the app is clustered.
 */
@Component
public class PresenceService {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/rooms\\.([^./]+)$");

    private final RoomCatalog roomCatalog;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptionDestinations = new ConcurrentHashMap<>();

    public PresenceService(RoomCatalog roomCatalog, SimpMessagingTemplate messagingTemplate) {
        this.roomCatalog = roomCatalog;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String roomId = roomIdFromChatTopic(destination);
        if (roomId == null) {
            return;
        }
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        if (user == null || sessionId == null) {
            return;
        }
        sessionUsers.put(sessionId, user.getName());
        sessionRooms.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(roomId);
        roomSessions.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        if (subscriptionId != null) {
            subscriptionDestinations.put(sessionId + ":" + subscriptionId, destination);
        }
        broadcast(roomId);
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        if (sessionId == null || subscriptionId == null) {
            return;
        }
        String destination = subscriptionDestinations.remove(sessionId + ":" + subscriptionId);
        String roomId = roomIdFromChatTopic(destination);
        if (roomId == null) {
            return;
        }
        leaveRoom(sessionId, roomId);
        broadcast(roomId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Set<String> rooms = sessionRooms.remove(sessionId);
        sessionUsers.remove(sessionId);
        subscriptionDestinations.keySet().removeIf(key -> key.startsWith(sessionId + ":"));
        if (rooms == null) {
            return;
        }
        for (String roomId : rooms) {
            Set<String> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(sessionId);
            }
            broadcast(roomId);
        }
    }

    public List<String> usersIn(String roomId) {
        return usernames(roomId);
    }

    private void leaveRoom(String sessionId, String roomId) {
        Set<String> rooms = sessionRooms.get(sessionId);
        if (rooms != null) {
            rooms.remove(roomId);
        }
        Set<String> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(sessionId);
        }
    }

    private void broadcast(String roomId) {
        messagingTemplate.convertAndSend("/topic/rooms." + roomId + ".presence", new PresenceEvent(roomId, usernames(roomId)));
    }

    private List<String> usernames(String roomId) {
        Set<String> sessions = roomSessions.getOrDefault(roomId, Set.of());
        return sessions.stream()
                .map(sessionUsers::get)
                .filter(name -> name != null)
                .distinct()
                .sorted()
                .toList();
    }

    private String roomIdFromChatTopic(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = ROOM_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        String roomId = matcher.group(1);
        return roomCatalog.find(roomId).map(room -> roomId).orElse(null);
    }
}
