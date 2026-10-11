package space.gilberthg.springwebsocketimpl;

import space.gilberthg.springwebsocketimpl.auth.TokenResponse;
import space.gilberthg.springwebsocketimpl.chat.ChatMessage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTests {

    @LocalServerPort
    int port;

    @Test
    void broadcastChatMessageToRoomSubscribers() throws Exception {
        String token = login("alice");
        StompSession session = connect(token);

        CompletableFuture<ChatMessage> received = new CompletableFuture<>();
        session.subscribe("/topic/rooms.general", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((ChatMessage) payload);
            }
        });
        Thread.sleep(200);

        session.send("/app/rooms.general.chat", Map.of("content", "hello from test"));

        ChatMessage message = received.get(5, TimeUnit.SECONDS);
        assertThat(message.sender()).isEqualTo("alice");
        assertThat(message.content()).isEqualTo("hello from test");
        assertThat(message.roomId()).isEqualTo("general");
        session.disconnect();
    }

    @Test
    void rejectConnectWithoutJwt() {
        assertThatThrownBy(() -> connectAsync(null).get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }

    @Test
    void rejectConnectWithInvalidJwt() {
        assertThatThrownBy(() -> connectAsync("not-a-valid-token").get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }

    private String login(String username) {
        TokenResponse response = RestClient.create()
                .post()
                .uri("http://localhost:" + port + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", "password"))
                .retrieve()
                .body(TokenResponse.class);
        assertThat(response).isNotNull();
        return response.token();
    }

    private StompSession connect(String token) throws Exception {
        return connectAsync(token).get(5, TimeUnit.SECONDS);
    }

    private CompletableFuture<StompSession> connectAsync(String token) {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        URI uri = URI.create("ws://localhost:" + port + "/ws");
        return client.connectAsync(uri, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
        });
    }
}
