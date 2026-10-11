package space.gilberthg.springwebsocketimpl.chat;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(UnknownRoomException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessage unknownRoom(UnknownRoomException exception) {
        return new ErrorMessage("unknown_room", exception.getMessage());
    }
}
