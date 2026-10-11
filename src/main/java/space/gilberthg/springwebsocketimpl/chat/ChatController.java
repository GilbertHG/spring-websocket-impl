package space.gilberthg.springwebsocketimpl.chat;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/rooms.{roomId}.chat")
    public void chat(
            @DestinationVariable String roomId,
            @Valid ChatSendRequest request,
            Principal principal) {
        chatService.send(roomId, principal.getName(), request.content());
    }

    @MessageMapping("/rooms.{roomId}.typing")
    public void typing(
            @DestinationVariable String roomId,
            @Valid TypingRequest request,
            Principal principal) {
        chatService.typing(roomId, principal.getName(), request.typing());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorMessage handle(Exception exception) {
        if (exception instanceof UnknownRoomException) {
            return new ErrorMessage("unknown_room", exception.getMessage());
        }
        return new ErrorMessage("error", exception.getMessage());
    }
}
