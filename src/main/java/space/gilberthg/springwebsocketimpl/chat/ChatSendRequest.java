package space.gilberthg.springwebsocketimpl.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(
        @NotBlank @Size(max = 2000) String content) {
}
